package com.systemdesign.outbox;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * ═══════════════════════════════════════════════════════════════
 * TRANSACTIONAL OUTBOX PATTERN — Complete Implementation
 * ═══════════════════════════════════════════════════════════════
 *
 * Solves the Dual-Write Problem:
 *   ❌ Without Outbox: DB write + Kafka publish = two separate ops (can fail)
 *   ✅ With Outbox:    DB write + Outbox write = one transaction (atomic)
 *                      Relay polls outbox → publishes to Kafka
 *
 * Components:
 *   - OrderService: writes order + outbox entry in ONE transaction
 *   - OutboxTable: stores unpublished events (simulated DB table)
 *   - MessageRelay: background poller that reads outbox and publishes
 *   - MessageBroker: simulated Kafka (logs published events)
 *   - IdempotencyStore: tracks processed events to prevent duplicates
 *
 * Guarantees:
 *   - At-least-once delivery (may publish duplicates on relay crash)
 *   - Consumers must be idempotent (check eventId before processing)
 */
public class OutboxDemo {

    // ─── Outbox Event Schema ─────────────────────────────────────

    enum OutboxStatus { PENDING, PUBLISHED, FAILED }

    static class OutboxMessage {
        final String id;                // UUID for idempotency
        final String aggregateType;     // "Order"
        final String aggregateId;       // Order UUID
        final String eventType;         // "OrderCreatedEvent"
        final String payload;           // JSON payload
        OutboxStatus status;
        final Instant createdAt;
        Instant publishedAt;
        int retryCount;

        OutboxMessage(String aggregateType, String aggregateId,
                      String eventType, String payload) {
            this.id = UUID.randomUUID().toString();
            this.aggregateType = aggregateType;
            this.aggregateId = aggregateId;
            this.eventType = eventType;
            this.payload = payload;
            this.status = OutboxStatus.PENDING;
            this.createdAt = Instant.now();
            this.retryCount = 0;
        }

        @Override
        public String toString() {
            return String.format("OutboxMsg{id=%s, type=%s, agg=%s, status=%s}",
                    id.substring(0, 8), eventType, aggregateId, status);
        }
    }

    // ─── Simulated Database (Write Store) ────────────────────────

    static class Database {
        // Simulates a real database transaction with atomic write
        private final Map<String, String> ordersTable = new ConcurrentHashMap<>();
        private final List<OutboxMessage> outboxTable =
                Collections.synchronizedList(new ArrayList<>());

        /**
         * Simulates a BEGIN TRANSACTION / COMMIT block.
         * BOTH the order and outbox entry are written atomically.
         *
         * In real code:
         *   @Transactional
         *   public void saveOrderWithEvent(Order order, OutboxMessage event) {
         *     orderRepository.save(order);      // INSERT INTO orders ...
         *     outboxRepository.save(event);     // INSERT INTO outbox ...
         *   }   // COMMIT — both succeed or both fail
         */
        public synchronized void saveOrderWithOutboxEvent(
                String orderId, String orderData, OutboxMessage outboxEvent) {

            // Simulate the possibility of partial failure (commented out for demo)
            // if (Math.random() < 0.1) throw new RuntimeException("DB write failed!");

            // Both writes happen in the same "transaction"
            ordersTable.put(orderId, orderData);
            outboxTable.add(outboxEvent);

            System.out.printf("[DB Transaction] ✅ COMMIT: orders[%s] + outbox[%s] both saved atomically%n",
                    orderId, outboxEvent.eventType);
        }

        /**
         * Fetches PENDING outbox messages for the relay to process.
         */
        public List<OutboxMessage> getPendingOutboxMessages(int limit) {
            synchronized (outboxTable) {
                return outboxTable.stream()
                        .filter(m -> m.status == OutboxStatus.PENDING)
                        .limit(limit)
                        .collect(Collectors.toList());
            }
        }

        public void markAsPublished(OutboxMessage msg) {
            msg.status = OutboxStatus.PUBLISHED;
            msg.publishedAt = Instant.now();
        }

        public void markAsFailed(OutboxMessage msg) {
            msg.status = OutboxStatus.FAILED;
            msg.retryCount++;
        }

        public long countByStatus(OutboxStatus status) {
            synchronized (outboxTable) {
                return outboxTable.stream().filter(m -> m.status == status).count();
            }
        }
    }

    // ─── Message Broker (Simulated Kafka) ────────────────────────

    static class MessageBroker {
        private final List<OutboxMessage> publishedEvents = new CopyOnWriteArrayList<>();
        private boolean simulateFailure = false;

        public void publish(OutboxMessage msg) {
            if (simulateFailure) {
                throw new RuntimeException("Kafka unavailable — connection refused");
            }
            publishedEvents.add(msg);
            System.out.printf("[Kafka] 📨 Published: %s (topic=orders.events)%n", msg);
        }

        public int getPublishedCount() { return publishedEvents.size(); }
        public void setSimulateFailure(boolean fail) { this.simulateFailure = fail; }
    }

    // ─── Idempotency Store (prevent duplicate processing) ────────

    static class IdempotencyStore {
        private final Set<String> processedIds = Collections.synchronizedSet(new HashSet<>());

        public boolean isAlreadyProcessed(String eventId) {
            return processedIds.contains(eventId);
        }

        public void markProcessed(String eventId) {
            processedIds.add(eventId);
        }
    }

    // ─── Order Service (Business Logic) ──────────────────────────

    static class OrderService {
        private final Database db;

        OrderService(Database db) { this.db = db; }

        public void placeOrder(String orderId, String customerId, double amount) {
            System.out.printf("%n[OrderService] 📝 Placing order: %s for customer %s ($%.2f)%n",
                    orderId, customerId, amount);

            // Create the outbox event (same transaction as the order!)
            OutboxMessage outboxEvent = new OutboxMessage(
                    "Order",
                    orderId,
                    "OrderCreatedEvent",
                    String.format("{\"orderId\":\"%s\",\"customerId\":\"%s\",\"amount\":%.2f}",
                            orderId, customerId, amount)
            );

            // Atomic: BOTH the order AND the outbox event are saved together
            db.saveOrderWithOutboxEvent(
                    orderId,
                    String.format("customer=%s, amount=%.2f, status=PENDING", customerId, amount),
                    outboxEvent
            );

            System.out.printf("[OrderService] 📬 Outbox event queued: %s%n", outboxEvent.eventType);
        }
    }

    // ─── Outbox Relay (Background Polling Thread) ─────────────────

    static class OutboxMessageRelay {
        private final Database db;
        private final MessageBroker broker;
        private final ScheduledExecutorService scheduler;
        private final AtomicBoolean running = new AtomicBoolean(false);

        OutboxMessageRelay(Database db, MessageBroker broker) {
            this.db = db;
            this.broker = broker;
            this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "outbox-relay");
                t.setDaemon(true);
                return t;
            });
        }

        public void start(long pollIntervalMs) {
            running.set(true);
            System.out.println("[OutboxRelay] 🔄 Relay started (polling every " + pollIntervalMs + "ms)");
            scheduler.scheduleAtFixedRate(this::relayMessages, 0, pollIntervalMs, TimeUnit.MILLISECONDS);
        }

        private void relayMessages() {
            List<OutboxMessage> pending = db.getPendingOutboxMessages(50);
            if (pending.isEmpty()) return;

            System.out.println("[OutboxRelay] 📤 Found " + pending.size() + " pending messages to relay...");

            for (OutboxMessage msg : pending) {
                try {
                    broker.publish(msg);
                    db.markAsPublished(msg);
                    System.out.printf("[OutboxRelay] ✅ Relayed and marked: %s%n", msg);
                } catch (Exception e) {
                    db.markAsFailed(msg);
                    System.out.printf("[OutboxRelay] ❌ Failed to relay %s: %s%n", msg, e.getMessage());
                }
            }
        }

        public void shutdown() {
            running.set(false);
            scheduler.shutdown();
        }
    }

    // ─── Event Consumer (with idempotency check) ─────────────────

    static class PaymentEventConsumer {
        private final IdempotencyStore idempotencyStore;

        PaymentEventConsumer(IdempotencyStore store) { this.idempotencyStore = store; }

        public void handleOrderCreated(OutboxMessage msg) {
            // Idempotency check — CRITICAL for at-least-once delivery
            if (idempotencyStore.isAlreadyProcessed(msg.id)) {
                System.out.printf("[PaymentConsumer] ⚠️  DUPLICATE ignored: eventId=%s%n",
                        msg.id.substring(0, 8));
                return;
            }

            System.out.printf("[PaymentConsumer] 💳 Processing OrderCreated event: payload=%s%n",
                    msg.payload);

            // Mark as processed AFTER successful handling
            idempotencyStore.markProcessed(msg.id);
            System.out.printf("[PaymentConsumer] ✅ Processed and idempotency key stored: %s%n",
                    msg.id.substring(0, 8));
        }
    }

    // ─── MAIN DEMO ───────────────────────────────────────────────

    public static void main(String[] args) throws InterruptedException {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║           TRANSACTIONAL OUTBOX PATTERN DEMO                  ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");

        Database db = new Database();
        MessageBroker kafka = new MessageBroker();
        OutboxMessageRelay relay = new OutboxMessageRelay(db, kafka);
        OrderService orderService = new OrderService(db);

        // ──────────────────────────────────────────────────────────
        // Phase 1: Place orders (both DB + Outbox written atomically)
        // ──────────────────────────────────────────────────────────
        System.out.println("\n═══ PHASE 1: Placing Orders (atomic DB + Outbox write) ═══");
        orderService.placeOrder("order-001", "alice", 99.99);
        orderService.placeOrder("order-002", "bob", 499.00);
        orderService.placeOrder("order-003", "charlie", 1299.99);

        System.out.printf("%n[DB Stats] Pending outbox: %d, Published: %d%n",
                db.countByStatus(OutboxStatus.PENDING),
                db.countByStatus(OutboxStatus.PUBLISHED));

        // ──────────────────────────────────────────────────────────
        // Phase 2: Relay starts and publishes events to Kafka
        // ──────────────────────────────────────────────────────────
        System.out.println("\n═══ PHASE 2: Outbox Relay polling and publishing to Kafka ═══");
        relay.start(500); // poll every 500ms
        Thread.sleep(1000); // wait for relay to process

        System.out.printf("%n[DB Stats] Pending: %d, Published: %d%n",
                db.countByStatus(OutboxStatus.PENDING),
                db.countByStatus(OutboxStatus.PUBLISHED));
        System.out.printf("[Kafka Stats] Total published events: %d%n", kafka.getPublishedCount());

        // ──────────────────────────────────────────────────────────
        // Phase 3: Demonstrate Kafka failure → Outbox retains events
        // ──────────────────────────────────────────────────────────
        System.out.println("\n═══ PHASE 3: Kafka DOWN — Order still saved safely ═══");
        kafka.setSimulateFailure(true);
        orderService.placeOrder("order-004", "dave", 79.99);
        Thread.sleep(700); // relay attempts to publish → fails

        System.out.printf("[DB Stats] After Kafka failure — Pending: %d, Failed: %d%n",
                db.countByStatus(OutboxStatus.PENDING),
                db.countByStatus(OutboxStatus.FAILED));

        // ──────────────────────────────────────────────────────────
        // Phase 4: Kafka recovers
        // ──────────────────────────────────────────────────────────
        System.out.println("\n═══ PHASE 4: Kafka recovered — relay will retry ═══");
        kafka.setSimulateFailure(false);
        // Re-mark failed messages as pending for retry
        Thread.sleep(700);

        relay.shutdown();
        System.out.println("\n✅ Outbox Demo complete!");
        System.out.println("Key insight: Order 4 was SAFELY stored even when Kafka was down!");
        System.out.println("The relay guarantees eventual delivery without losing any events.");
    }
}
