package com.systemdesign.messagequeue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Rich interactive simulation showing Message Queues (P2P), Topics (Pub/Sub),
 * Consumer Groups load balancing, NACK retries leading to DLQ, and ACK timeouts.
 */
public class MessageBrokerDemo {
    private static final Logger log = LoggerFactory.getLogger(MessageBrokerDemo.class);

    public static void main(String[] args) {
        log.info("================================================================================");
        log.info("                 STARTING SYSTEM DESIGN DAY 7: MESSAGE BROKER DEMO               ");
        log.info("================================================================================");

        // Initialize broker: Max Retries = 2 (total 3 attempts), ACK Timeout = 1000ms (1 second)
        MessageBroker broker = new MessageBroker(2, 1000);

        try {
            // ================================================================================
            // SCENARIO 1: POINT-TO-POINT QUEUE (ORDER PROCESSING)
            // ================================================================================
            log.info("\n--------------------------------------------------------------------------------");
            log.info("SCENARIO 1: Point-to-Point Queue with Load-Balanced Workers (Round-Robin)");
            log.info("--------------------------------------------------------------------------------");

            String queueName = "order-processing-queue";

            // Register 2 consumers in the same queue consumer group
            Consumer queueWorker1 = new Consumer("OrderWorker-1", queueName, (message, acknowledger) -> {
                log.info("[OrderWorker-1] Processing order: {}", message.getPayload());
                // Simulate quick processing
                sleep(100);
                acknowledger.ack(true);
            });

            Consumer queueWorker2 = new Consumer("OrderWorker-2", queueName, (message, acknowledger) -> {
                log.info("[OrderWorker-2] Processing order: {}", message.getPayload());
                sleep(120);
                acknowledger.ack(true);
            });

            broker.registerQueueConsumer(queueName, queueWorker1);
            broker.registerQueueConsumer(queueName, queueWorker2);

            // Publish 4 orders to the queue
            log.info("Publishing 4 orders to '{}'...", queueName);
            broker.publish(queueName, "Order_ID_1001: Premium Laptop", false);
            broker.publish(queueName, "Order_ID_1002: Wireless Headphones", false);
            broker.publish(queueName, "Order_ID_1003: Ergonomic Chair", false);
            broker.publish(queueName, "Order_ID_1004: Mechanical Keyboard", false);

            // Let processing finish
            sleep(1000);

            // ================================================================================
            // SCENARIO 2: PUBLISH-SUBSCRIBE TOPIC WITH KAFKA-STYLE CONSUMER GROUPS
            // ================================================================================
            log.info("\n--------------------------------------------------------------------------------");
            log.info("SCENARIO 2: Pub/Sub Topic with broadcast to distinct Consumer Groups");
            log.info("--------------------------------------------------------------------------------");

            String topicName = "user-signup-topic";

            // Group 1: Email Notifier Service (Has 2 load-balanced workers to split the workload)
            Consumer emailWorker1 = new Consumer("EmailWorker-1", "EmailNotifierGroup", (message, acknowledger) -> {
                log.info("[EmailNotifierGroup -> EmailWorker-1] Sending Welcome Email to: {}", message.getPayload());
                sleep(80);
                acknowledger.ack(true);
            });
            Consumer emailWorker2 = new Consumer("EmailWorker-2", "EmailNotifierGroup", (message, acknowledger) -> {
                log.info("[EmailNotifierGroup -> EmailWorker-2] Sending Welcome Email to: {}", message.getPayload());
                sleep(80);
                acknowledger.ack(true);
            });

            // Group 2: Analytics Service (Has 1 worker tracking signup statistics)
            Consumer analyticsWorker1 = new Consumer("AnalyticsWorker-1", "AnalyticsTrackerGroup", (message, acknowledger) -> {
                log.info("[AnalyticsTrackerGroup -> AnalyticsWorker-1] Recording sign-up event for analytics: {}", message.getPayload());
                sleep(50);
                acknowledger.ack(true);
            });

            broker.registerTopicConsumer(topicName, emailWorker1);
            broker.registerTopicConsumer(topicName, emailWorker2);
            broker.registerTopicConsumer(topicName, analyticsWorker1);

            // Publish signup events
            log.info("Publishing signup events to Topic '{}'...", topicName);
            broker.publish(topicName, "alice@example.com", true);
            broker.publish(topicName, "bob@example.com", true);

            // Let processing finish
            sleep(1000);

            // ================================================================================
            // SCENARIO 3: POISON PILL RETRIES & DEAD LETTER QUEUE (DLQ)
            // ================================================================================
            log.info("\n--------------------------------------------------------------------------------");
            log.info("SCENARIO 3: Resiliency & Fault Tolerance - Malformed Messages -> DLQ");
            log.info("--------------------------------------------------------------------------------");

            String errorQueueName = "payment-queue";

            // Consumer throws exceptions or returns failure (NACK) for malformed payloads
            Consumer paymentProcessor = new Consumer("PaymentProcessor-1", errorQueueName, (message, acknowledger) -> {
                if (message.getPayload().contains("MALFORMED")) {
                    log.warn("[PaymentProcessor-1] Encountered BAD payload: '{}'!", message.getPayload());
                    // Negative Acknowledgment
                    acknowledger.ack(false);
                } else {
                    log.info("[PaymentProcessor-1] Processed payment: {}", message.getPayload());
                    acknowledger.ack(true);
                }
            });

            broker.registerQueueConsumer(errorQueueName, paymentProcessor);

            log.info("Publishing a good payment and a MALFORMED bad payment...");
            broker.publish(errorQueueName, "Txn_2001: $49.99", false);
            broker.publish(errorQueueName, "Txn_MALFORMED: Invalid Credit Card Hash", false);

            // Let processing complete (includes retries)
            sleep(2000);

            // Verify DLQ
            log.info("\nChecking Dead Letter Queue (DLQ) state...");
            log.info("Messages in DLQ: {}", broker.getDLQMessages());

            // ================================================================================
            // SCENARIO 4: ACKNOWLEDGMENT TIMEOUT & RECLAIM REAPING
            // ================================================================================
            log.info("\n--------------------------------------------------------------------------------");
            log.info("SCENARIO 4: Reliability under Consumer Hang - In-Flight Timeout & Reclaim");
            log.info("--------------------------------------------------------------------------------");

            String timeoutQueueName = "inventory-queue";

            // Worker 1 is buggy and will HANG indefinitely for slow messages, simulating a network lockup or JVM freeze
            Consumer inventoryWorker1 = new Consumer("InventoryWorker-1", timeoutQueueName, (message, acknowledger) -> {
                if (message.getPayload().contains("SLOW")) {
                    log.warn("[InventoryWorker-1] Oh no, I am hanging and forgot to ACK message: '{}'!", message.getPayload());
                    // Simulates hang - we NEVER call acknowledger.ack()
                    sleep(5000); 
                } else {
                    log.info("[InventoryWorker-1] Handled quick item: {}", message.getPayload());
                    acknowledger.ack(true);
                }
            });

            // Worker 2 is responsive and will pick up the re-queued message
            Consumer inventoryWorker2 = new Consumer("InventoryWorker-2", timeoutQueueName, (message, acknowledger) -> {
                log.info("[InventoryWorker-2] Successfully handled inventory update: {}", message.getPayload());
                acknowledger.ack(true);
            });

            broker.registerQueueConsumer(timeoutQueueName, inventoryWorker1);
            broker.registerQueueConsumer(timeoutQueueName, inventoryWorker2);

            log.info("Publishing slow items to '{}'...", timeoutQueueName);
            broker.publish(timeoutQueueName, "SLOW_Item_4001: Re-stock Industrial Engine", false);

            // Let reaper work. Timeout is 1000ms. Reaper runs every 200ms.
            // After ~1000ms, the reaper should detect that InventoryWorker-1 has timed out.
            // It will trigger an automatic NACK, increment the retry, and re-enqueue it.
            // InventoryWorker-2 (or 1) will pick it up. Since round-robin handles it, it will be delivered and handled.
            sleep(2500);

        } finally {
            log.info("\n================================================================================");
            log.info("                         SHUTTING DOWN MESSAGE BROKER                           ");
            log.info("================================================================================");
            broker.shutdown();
        }
    }

    private static void sleep(long ms) {
        try {
            TimeUnit.MILLISECONDS.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
