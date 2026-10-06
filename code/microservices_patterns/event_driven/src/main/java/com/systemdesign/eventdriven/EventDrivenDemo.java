package com.systemdesign.eventdriven;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * ═══════════════════════════════════════════════════════════════
 * EVENT-DRIVEN ARCHITECTURE — Complete Implementation
 * ═══════════════════════════════════════════════════════════════
 *
 * Demonstrates:
 *   - Publish/Subscribe with a simulated Event Bus (Kafka-like)
 *   - Consumer Groups (one message → processed by ONE instance in group)
 *   - Event partitioning by key
 *   - Dead Letter Queue (DLQ) for failed events
 *   - At-least-once delivery with retry
 *   - Event schema with eventId, correlationId, version
 *
 * Services:
 *   - OrderService (producer)
 *   - PaymentService (consumer, consumer group: "payments")
 *   - InventoryService (consumer, consumer group: "inventory")
 *   - NotificationService (consumer, consumer group: "notifications")
 */
public class EventDrivenDemo {

    // ─── Base Event ─────────────────────────────────────────────

    static abstract class DomainEvent {
        final String eventId;           // Unique ID — for idempotency
        final String eventType;         // "OrderPlacedEvent"
        final String aggregateId;       // "order-123" — used as partition key
        final String correlationId;     // Trace through all services
        final int version;              // Schema version
        final Instant timestamp;

        protected DomainEvent(String eventType, String aggregateId) {
            this.eventId = UUID.randomUUID().toString();
            this.eventType = eventType;
            this.aggregateId = aggregateId;
            this.correlationId = UUID.randomUUID().toString().substring(0, 8);
            this.version = 1;
            this.timestamp = Instant.now();
        }

        @Override
        public String toString() {
            return String.format("[%s | eventId=%s | agg=%s | ts=%s]",
                    eventType, eventId.substring(0, 8), aggregateId, timestamp);
        }
    }

    // ─── Concrete Events (past-tense naming!) ────────────────────

    static class OrderPlacedEvent extends DomainEvent {
        final String customerId;
        final double amount;
        final String productId;
        final int quantity;

        OrderPlacedEvent(String orderId, String customerId, double amount,
                         String productId, int quantity) {
            super("OrderPlacedEvent", orderId);
            this.customerId = customerId;
            this.amount = amount;
            this.productId = productId;
            this.quantity = quantity;
        }
    }

    static class PaymentProcessedEvent extends DomainEvent {
        final String orderId;
        final double amount;
        final String paymentStatus; // "SUCCESS" | "FAILED"

        PaymentProcessedEvent(String orderId, double amount, String paymentStatus) {
            super("PaymentProcessedEvent", orderId);
            this.orderId = orderId;
            this.amount = amount;
            this.paymentStatus = paymentStatus;
        }
    }

    // ─── Event Bus (Simulated Kafka with Consumer Groups + DLQ) ──

    static class EventBus {
        // topic → partition → ordered list of events
        private final Map<String, List<List<DomainEvent>>> topics = new ConcurrentHashMap<>();

        // Dead Letter Queue — events that couldn't be processed
        private final BlockingQueue<DLQEntry> deadLetterQueue = new LinkedBlockingQueue<>();

        // consumer group → topic → partition offset
        private final Map<String, Map<String, Integer[]>> consumerGroupOffsets = new ConcurrentHashMap<>();

        private final int partitionCount;
        private final ExecutorService executor;

        EventBus(int partitionCount) {
            this.partitionCount = partitionCount;
            this.executor = Executors.newCachedThreadPool();
        }

        public void createTopic(String topic) {
            topics.computeIfAbsent(topic, k -> {
                List<List<DomainEvent>> partitions = new ArrayList<>();
                for (int i = 0; i < partitionCount; i++) {
                    partitions.add(Collections.synchronizedList(new ArrayList<>()));
                }
                return partitions;
            });
            System.out.println("[EventBus] 📋 Topic created: " + topic +
                    " (" + partitionCount + " partitions)");
        }

        /**
         * Publish an event. Partitioned by aggregateId for ordering guarantees.
         */
        public void publish(String topic, DomainEvent event) {
            List<List<DomainEvent>> partitions = topics.get(topic);
            if (partitions == null) throw new IllegalStateException("Topic not found: " + topic);

            // Partition by hash of aggregateId (same orderId always → same partition)
            int partition = Math.abs(event.aggregateId.hashCode()) % partitionCount;
            partitions.get(partition).add(event);

            System.out.printf("[EventBus] 📤 Published to %s/partition-%d: %s%n",
                    topic, partition, event);
        }

        /**
         * Subscribe a consumer group to a topic.
         * Within a group, each partition is consumed by ONE instance (like Kafka).
         */
        public void subscribe(String topic, String consumerGroup,
                              Consumer<DomainEvent> handler, int maxRetries) {
            // Track offsets per partition
            List<List<DomainEvent>> partitions = topics.get(topic);
            Integer[] offsets = new Integer[partitionCount];
            Arrays.fill(offsets, 0);

            consumerGroupOffsets
                    .computeIfAbsent(consumerGroup, k -> new ConcurrentHashMap<>())
                    .put(topic, offsets);

            // Start a consumer thread that polls all partitions
            executor.submit(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    for (int p = 0; p < partitionCount; p++) {
                        List<DomainEvent> partition = partitions.get(p);
                        int offset = offsets[p];

                        while (offset < partition.size()) {
                            DomainEvent event = partition.get(offset);
                            processWithRetry(event, handler, maxRetries,
                                    consumerGroup, topic, p, offset);
                            offsets[p] = ++offset;
                        }
                    }
                    try { Thread.sleep(100); } catch (InterruptedException e) { break; }
                }
            });
        }

        private void processWithRetry(DomainEvent event, Consumer<DomainEvent> handler,
                                      int maxRetries, String group, String topic, int partition, int offset) {
            int attempts = 0;
            Exception lastException = null;

            while (attempts <= maxRetries) {
                try {
                    handler.accept(event);
                    return; // Success
                } catch (Exception e) {
                    lastException = e;
                    attempts++;
                    if (attempts <= maxRetries) {
                        System.out.printf("[EventBus] ⚠️  Retry %d/%d for %s in group=%s%n",
                                attempts, maxRetries, event.eventType, group);
                        try { Thread.sleep(100L * attempts); } catch (InterruptedException ignored) {}
                    }
                }
            }

            // Move to DLQ after exhausting retries
            deadLetterQueue.add(new DLQEntry(event, group, topic, lastException));
            System.out.printf("[EventBus] ☠️  DLQ: Event %s moved to Dead Letter Queue after %d retries%n",
                    event.eventId.substring(0, 8), maxRetries);
        }

        public void printDLQ() {
            System.out.println("\n[DLQ] Dead Letter Queue contents:");
            if (deadLetterQueue.isEmpty()) {
                System.out.println("  (empty — all events processed successfully!)");
            }
            deadLetterQueue.forEach(e ->
                    System.out.printf("  ☠️  %s | group=%s | error=%s%n",
                            e.event.eventType, e.consumerGroup, e.exception.getMessage()));
        }

        record DLQEntry(DomainEvent event, String consumerGroup, String topic, Exception exception) {}

        public void shutdown() { executor.shutdown(); }
    }

    // ─── Microservices (Producers & Consumers) ───────────────────

    static class OrderService {
        private final EventBus eventBus;

        OrderService(EventBus bus) { this.eventBus = bus; }

        public void placeOrder(String orderId, String customerId,
                               String productId, int qty, double amount) {
            System.out.printf("%n[OrderService] 📝 Order placed: %s%n", orderId);
            eventBus.publish("orders", new OrderPlacedEvent(
                    orderId, customerId, amount, productId, qty));
        }
    }

    static class PaymentService {
        private boolean failNextCall = false;

        public void handleOrderPlaced(DomainEvent event) {
            if (!(event instanceof OrderPlacedEvent e)) return;
            if (failNextCall) {
                failNextCall = false;
                throw new RuntimeException("Payment gateway timeout");
            }
            System.out.printf("[PaymentService] 💳 Processing payment for order=%s, amount=$%.2f%n",
                    e.aggregateId, e.amount);
        }

        public void setFailNext(boolean fail) { this.failNextCall = fail; }
    }

    static class InventoryService {
        public void handleOrderPlaced(DomainEvent event) {
            if (!(event instanceof OrderPlacedEvent e)) return;
            System.out.printf("[InventoryService] 📦 Reserving %d units of %s for order=%s%n",
                    e.quantity, e.productId, e.aggregateId);
        }
    }

    static class NotificationService {
        public void handleOrderPlaced(DomainEvent event) {
            if (!(event instanceof OrderPlacedEvent e)) return;
            System.out.printf("[NotificationService] 📧 Sending 'Order Confirmed' email " +
                    "to customer=%s for order=%s%n", e.customerId, e.aggregateId);
        }
    }

    // ─── MAIN DEMO ───────────────────────────────────────────────

    public static void main(String[] args) throws InterruptedException {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║          EVENT-DRIVEN ARCHITECTURE DEMO                      ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");

        // Setup event bus with 3 partitions (like Kafka topic with 3 partitions)
        EventBus eventBus = new EventBus(3);
        eventBus.createTopic("orders");

        // Create services
        OrderService orderSvc = new OrderService(eventBus);
        PaymentService paymentSvc = new PaymentService();
        InventoryService inventorySvc = new InventoryService();
        NotificationService notificationSvc = new NotificationService();

        // ─── Subscribe consumer groups ──────────────────────────
        System.out.println("\n═══ Setting up Consumer Groups ═══");

        // Each service is in its own consumer group
        // → Each service gets a copy of EVERY event (broadcast)
        eventBus.subscribe("orders", "payment-service", paymentSvc::handleOrderPlaced, 2);
        eventBus.subscribe("orders", "inventory-service", inventorySvc::handleOrderPlaced, 2);
        eventBus.subscribe("orders", "notification-service", notificationSvc::handleOrderPlaced, 2);

        // ─── Phase 1: Publish events ────────────────────────────
        System.out.println("\n═══ PHASE 1: Orders placed (events published to Kafka) ═══");
        orderSvc.placeOrder("order-001", "alice", "prod-iphone", 1, 999.99);
        orderSvc.placeOrder("order-002", "bob", "prod-mac", 1, 2499.99);
        orderSvc.placeOrder("order-003", "charlie", "prod-airpods", 2, 249.99);

        Thread.sleep(500); // Let consumers process

        // ─── Phase 2: DLQ demonstration ─────────────────────────
        System.out.println("\n═══ PHASE 2: Payment failure → DLQ demonstration ═══");
        paymentSvc.setFailNext(true); // Next call will fail
        orderSvc.placeOrder("order-004", "dave", "prod-watch", 1, 399.99);
        Thread.sleep(800); // Wait for retries + DLQ

        // ─── Print DLQ contents ─────────────────────────────────
        eventBus.printDLQ();

        eventBus.shutdown();
        System.out.println("\n✅ Event-Driven Architecture Demo complete!");
        System.out.println("Key insight: OrderService had NO knowledge of Payment/Inventory/Notification!");
        System.out.println("Adding a new service = just add a new consumer group. Zero code change in producers!");
    }
}
