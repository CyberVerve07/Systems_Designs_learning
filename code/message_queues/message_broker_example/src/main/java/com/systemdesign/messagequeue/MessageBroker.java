package com.systemdesign.messagequeue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Core thread-safe In-Memory Message Broker implementing point-to-point queues,
 * publish/subscribe topics with consumer groups, message reliability (ACK/NACK),
 * timeout reclaims, and Dead Letter Queue (DLQ) routing.
 */
public class MessageBroker {
    private static final Logger log = LoggerFactory.getLogger(MessageBroker.class);

    private final int maxRetries;
    private final long ackTimeoutMs;

    // Queue Name -> List of Queue Consumers
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<Consumer>> queueConsumers = new ConcurrentHashMap<>();
    
    // Queue Name -> Message Backlog (used when no consumers are active)
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<Message>> queueBacklog = new ConcurrentHashMap<>();

    // Topic Name -> (Consumer Group Name -> List of Consumers)
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, CopyOnWriteArrayList<Consumer>>> topicConsumers = new ConcurrentHashMap<>();
    
    // Topic Name -> (Consumer Group Name -> Message Backlog for that group)
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentLinkedQueue<Message>>> topicGroupBacklog = new ConcurrentHashMap<>();

    // Message ID -> In-Flight State
    private final ConcurrentHashMap<String, InFlightMessage> inFlightMessages = new ConcurrentHashMap<>();

    // Special Dead Letter Queue (DLQ)
    private final ConcurrentLinkedQueue<Message> deadLetterQueue = new ConcurrentLinkedQueue<>();

    // Round-robin indexers: Key is destination or "topicName:groupName" -> atomic counter
    private final ConcurrentHashMap<String, AtomicInteger> roundRobinIndices = new ConcurrentHashMap<>();

    // Executor pools
    private final ExecutorService workerPool = Executors.newCachedThreadPool();
    private final ScheduledExecutorService reaperScheduler = Executors.newSingleThreadScheduledExecutor();

    public MessageBroker(int maxRetries, long ackTimeoutMs) {
        this.maxRetries = maxRetries;
        this.ackTimeoutMs = ackTimeoutMs;
        
        // Start the background Timeout Reaper to scan and reclaim deadlocked/un-acked messages
        this.reaperScheduler.scheduleAtFixedRate(this::reclaimTimedOutMessages, 200, 200, TimeUnit.MILLISECONDS);
        log.info("Message Broker initialized [Max Retries: {}, ACK Timeout: {}ms]", maxRetries, ackTimeoutMs);
    }

    /**
     * Shuts down background threads.
     */
    public void shutdown() {
        reaperScheduler.shutdown();
        workerPool.shutdown();
        try {
            if (!workerPool.awaitTermination(2, TimeUnit.SECONDS)) {
                workerPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            workerPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("Message Broker shutdown complete.");
    }

    // ==========================================
    // CONSUMER REGISTRATION
    // ==========================================

    /**
     * Register a consumer for a Point-to-Point Queue.
     */
    public void registerQueueConsumer(String queueName, Consumer consumer) {
        queueConsumers.computeIfAbsent(queueName, k -> new CopyOnWriteArrayList<>()).add(consumer);
        log.info("Registered Queue Consumer: {} on Queue: '{}'", consumer.getId(), queueName);
        
        // Drain any messages that accumulated in the backlog while no consumers were active
        triggerQueueDrain(queueName);
    }

    /**
     * Register a consumer to a Publish/Subscribe Topic within a specific Consumer Group.
     */
    public void registerTopicConsumer(String topicName, Consumer consumer) {
        topicConsumers.computeIfAbsent(topicName, k -> new ConcurrentHashMap<>())
                      .computeIfAbsent(consumer.getGroupName(), g -> new CopyOnWriteArrayList<>())
                      .add(consumer);
        log.info("Registered Topic Consumer: {} in Group: '{}' on Topic: '{}'", 
                consumer.getId(), consumer.getGroupName(), topicName);

        // Drain any messages that accumulated in the backlog for this group
        triggerTopicGroupDrain(topicName, consumer.getGroupName());
    }

    // ==========================================
    // MESSAGE PUBLISHING
    // ==========================================

    /**
     * Publishes a message to a Queue or Topic.
     */
    public void publish(String destination, String payload, boolean isTopic) {
        Message message = new Message(payload, destination);
        if (isTopic) {
            publishToTopic(message);
        } else {
            publishToQueue(message);
        }
    }

    private void publishToQueue(Message message) {
        String queueName = message.getDestination();
        ConcurrentLinkedQueue<Message> backlog = queueBacklog.computeIfAbsent(queueName, k -> new ConcurrentLinkedQueue<>());
        backlog.add(message);
        triggerQueueDrain(queueName);
    }

    private void publishToTopic(Message message) {
        String topicName = message.getDestination();
        ConcurrentHashMap<String, CopyOnWriteArrayList<Consumer>> groups = topicConsumers.get(topicName);
        
        if (groups == null || groups.isEmpty()) {
            log.warn("Publish: No active consumer groups for Topic '{}'. Discarding message: {}", topicName, message.getId());
            return;
        }

        // Broadcast a clone/copy of the message to each registered Consumer Group
        for (String groupName : groups.keySet()) {
            // Each group gets its own virtual copy of the message for independent tracking
            Message groupCopy = new Message(message.getPayload(), message.getDestination());
            
            ConcurrentLinkedQueue<Message> backlog = topicGroupBacklog
                    .computeIfAbsent(topicName, k -> new ConcurrentHashMap<>())
                    .computeIfAbsent(groupName, g -> new ConcurrentLinkedQueue<>());
            
            backlog.add(groupCopy);
            triggerTopicGroupDrain(topicName, groupName);
        }
    }

    // ==========================================
    // BACKLOG DRAINING & ROUND-ROBIN DISPATCHING
    // ==========================================

    private void triggerQueueDrain(String queueName) {
        ConcurrentLinkedQueue<Message> backlog = queueBacklog.get(queueName);
        if (backlog == null || backlog.isEmpty()) return;

        CopyOnWriteArrayList<Consumer> consumers = queueConsumers.get(queueName);
        if (consumers == null || consumers.isEmpty()) return;

        // Keep delivering as long as there are messages in the queue
        while (!backlog.isEmpty()) {
            // Re-fetch consumers to verify active listeners
            consumers = queueConsumers.get(queueName);
            if (consumers == null || consumers.isEmpty()) break;

            Message msg = backlog.poll();
            if (msg == null) break;

            // Round-robin load balancing
            Consumer selectedConsumer = selectRoundRobin(queueName, consumers);
            dispatchMessage(msg, selectedConsumer);
        }
    }

    private void triggerTopicGroupDrain(String topicName, String groupName) {
        ConcurrentHashMap<String, ConcurrentLinkedQueue<Message>> groupBacklogs = topicGroupBacklog.get(topicName);
        if (groupBacklogs == null) return;
        ConcurrentLinkedQueue<Message> backlog = groupBacklogs.get(groupName);
        if (backlog == null || backlog.isEmpty()) return;

        ConcurrentHashMap<String, CopyOnWriteArrayList<Consumer>> groups = topicConsumers.get(topicName);
        if (groups == null) return;
        CopyOnWriteArrayList<Consumer> consumers = groups.get(groupName);
        if (consumers == null || consumers.isEmpty()) return;

        // Deliver messages sequentially to available consumers in this group
        while (!backlog.isEmpty()) {
            consumers = groups.get(groupName);
            if (consumers == null || consumers.isEmpty()) break;

            Message msg = backlog.poll();
            if (msg == null) break;

            // Load balance among the consumers within this group
            String indexKey = topicName + ":" + groupName;
            Consumer selectedConsumer = selectRoundRobin(indexKey, consumers);
            dispatchMessage(msg, selectedConsumer);
        }
    }

    private Consumer selectRoundRobin(String key, List<Consumer> consumers) {
        AtomicInteger indexer = roundRobinIndices.computeIfAbsent(key, k -> new AtomicInteger(0));
        int index = Math.abs(indexer.getAndIncrement()) % consumers.size();
        return consumers.get(index);
    }

    private void dispatchMessage(Message message, Consumer consumer) {
        // Mark as In-Flight
        message.setLastAttemptTimestamp(System.currentTimeMillis());
        InFlightMessage inFlight = new InFlightMessage(message, consumer, ackTimeoutMs);
        inFlightMessages.put(message.getId(), inFlight);

        log.info("Dispatch: Message [{}] -> Consumer '{}' [{}]", 
                message.getId().substring(0, 8), consumer.getId(), consumer.getGroupName());

        // Process message asynchronously using worker threads so the broker main loop is non-blocking
        workerPool.submit(() -> {
            try {
                consumer.getHandler().onMessage(message, success -> acknowledge(message.getId(), success));
            } catch (Exception e) {
                log.error("Consumer Handler Error: Exception thrown by Consumer '{}' during message '{}' processing. Treating as NACK.", 
                        consumer.getId(), message.getId().substring(0, 8), e);
                acknowledge(message.getId(), false);
            }
        });
    }

    // ==========================================
    // ACKNOWLEDGMENT HANDLING (RETRY & DLQ)
    // ==========================================

    private void acknowledge(String messageId, boolean success) {
        InFlightMessage inFlight = inFlightMessages.remove(messageId);
        if (inFlight == null) {
            // Already ACKed or reclaimed due to timeout
            return;
        }

        Message msg = inFlight.getMessage();
        Consumer consumer = inFlight.getConsumer();

        if (success) {
            log.info("ACK: Message [{}] successfully processed by '{}' [{}]", 
                    msg.getId().substring(0, 8), consumer.getId(), consumer.getGroupName());
        } else {
            // NACK or failure
            log.warn("NACK: Message [{}] failed processing at '{}' [{}] (Retries so far: {}/{})", 
                    msg.getId().substring(0, 8), consumer.getId(), consumer.getGroupName(), msg.getRetryCount(), maxRetries);
            
            handleFailure(msg, consumer.getGroupName());
        }
    }

    private void handleFailure(Message msg, String groupName) {
        if (msg.getRetryCount() < maxRetries) {
            msg.incrementRetryCount();
            log.info("Retry: Re-queuing Message [{}] for group '{}'. Attempt: {}/{}", 
                    msg.getId().substring(0, 8), groupName, msg.getRetryCount() + 1, maxRetries + 1);
            
            // Re-queue the message to its backlog for retry
            requeueMessage(msg, groupName);
        } else {
            // Max retries exceeded -> Route to Dead Letter Queue (DLQ)
            deadLetterQueue.add(msg);
            log.error("DLQ: Message [{}] EXCEEDED max retries ({}). Sent to DEAD LETTER QUEUE (DLQ)!!! Payload: '{}'", 
                    msg.getId().substring(0, 8), maxRetries, msg.getPayload());
        }
    }

    private void requeueMessage(Message message, String groupName) {
        String dest = message.getDestination();
        boolean isTopic = topicConsumers.containsKey(dest);

        if (isTopic) {
            // Put it at the front of this specific consumer group's backlog
            ConcurrentLinkedQueue<Message> backlog = topicGroupBacklog
                    .computeIfAbsent(dest, k -> new ConcurrentHashMap<>())
                    .computeIfAbsent(groupName, g -> new ConcurrentLinkedQueue<>());
            
            // We can add it back to the backlog. In a real queue, we might prepend, but ConcurrentLinkedQueue is append-only.
            // That is fine since other active elements will drain and this will be retried next.
            backlog.add(message);
            triggerTopicGroupDrain(dest, groupName);
        } else {
            // Point-to-point Queue
            ConcurrentLinkedQueue<Message> backlog = queueBacklog.computeIfAbsent(dest, k -> new ConcurrentLinkedQueue<>());
            backlog.add(message);
            triggerQueueDrain(dest);
        }
    }

    // ==========================================
    // TIMEOUT RECLAIMER (REAPER BACKGROUND THREAD)
    // ==========================================

    private void reclaimTimedOutMessages() {
        for (Map.Entry<String, InFlightMessage> entry : inFlightMessages.entrySet()) {
            InFlightMessage inFlight = entry.getValue();
            if (inFlight.isTimedOut()) {
                // Double check we are the ones removing it to prevent race condition with late ACKs
                if (inFlightMessages.remove(entry.getKey(), inFlight)) {
                    Message msg = inFlight.getMessage();
                    Consumer consumer = inFlight.getConsumer();
                    
                    log.warn("TIMEOUT: Message [{}] timed out at Consumer '{}' [{}]! (No ACK/NACK received within {}ms)", 
                            msg.getId().substring(0, 8), consumer.getId(), consumer.getGroupName(), inFlight.getTimeoutMs());
                    
                    handleFailure(msg, consumer.getGroupName());
                }
            }
        }
    }

    // ==========================================
    // GETTERS FOR VERIFICATION / MONITORING
    // ==========================================

    public List<Message> getDLQMessages() {
        return new ArrayList<>(deadLetterQueue);
    }

    public int getInFlightCount() {
        return inFlightMessages.size();
    }
}
