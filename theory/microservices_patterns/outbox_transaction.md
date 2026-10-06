# 📖 Transactional Outbox Pattern

## 1. The Dual-Write Problem

After processing a business command (e.g., placing an order), you need to:
1. Save the order to the database
2. Publish an `OrderCreatedEvent` to Kafka/RabbitMQ so other services react

```java
// ❌ NAIVE (BROKEN) APPROACH — Don't do this!
public void placeOrder(Order order) {
    orderRepository.save(order);        // Step 1: DB write
    kafkaProducer.send("orders", event); // Step 2: Kafka publish
    // What if Kafka is down? Step 1 succeeded but Step 2 failed!
    // Your DB says "order exists" but Payment Service never received the event!
}
```

This is the **Dual-Write Problem** — two separate systems, two separate writes, no atomicity.

---

## 2. Failure Scenarios

```
SCENARIO A: DB succeeds, Kafka publish fails
  Result: Order exists in DB, but no downstream services notified
          → Payment never collected, Inventory never reserved
          → Silent data inconsistency 💀

SCENARIO B: App crashes between DB write and Kafka publish  
  Result: Same as Scenario A

SCENARIO C: Kafka succeeds but then you try DB write and it fails
  Result: Event published but no order in DB
          → Ghost event — payment deducted for a non-existent order 💀
```

---

## 3. The Outbox Pattern Solution

> **Key Insight**: Your database can guarantee atomicity within a single transaction.  
> Write **both** the business data AND the event to the **same database** in one transaction.

```
                    ┌─────────────────────────────────┐
                    │         Order Service            │
                    │                                  │
                    │  ┌──────────────────────────┐   │
                    │  │   BEGIN TRANSACTION       │   │
                    │  │                          │   │
                    │  │  INSERT INTO orders ...  │   │
                    │  │  INSERT INTO outbox ...  │   │← OUTBOX TABLE
                    │  │                          │   │
                    │  │   COMMIT                 │   │
                    │  └──────────────────────────┘   │
                    └─────────────────────────────────┘
                                    │
                                    │ Background: Relay Process
                                    ▼
                    ┌────────────────────────────────┐
                    │  Outbox Relay (Message Relay)  │
                    │  - Polls outbox table          │
                    │  - Reads unpublished events    │
                    │  - Publishes to Kafka          │
                    │  - Marks events as published   │
                    └────────────────────────────────┘
                                    │
                                    ▼
                    ┌────────────────────────────────┐
                    │         Kafka Topic            │
                    └────────────────────────────────┘
```

**The crucial guarantee**: If the DB transaction commits, BOTH the order record AND the outbox entry exist. The relay will eventually publish the event to Kafka. It might retry, but it will **never lose** the event.

---

## 4. Outbox Table Schema

```sql
CREATE TABLE outbox (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type  VARCHAR(100) NOT NULL,  -- 'Order', 'Payment'
    aggregate_id    VARCHAR(100) NOT NULL,  -- e.g., order UUID
    event_type      VARCHAR(100) NOT NULL,  -- 'OrderCreatedEvent'
    payload         JSONB        NOT NULL,  -- Full event data as JSON
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING', 
                                            -- PENDING | PUBLISHED | FAILED
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    published_at    TIMESTAMP,              -- When it was sent to Kafka
    retry_count     INT          NOT NULL DEFAULT 0
);

-- Index for efficient polling by relay
CREATE INDEX idx_outbox_status ON outbox(status, created_at);
```

---

## 5. Two Ways to Implement the Relay

### A. Polling-Based Relay (Simpler)

```
Every 100ms:
  SELECT * FROM outbox WHERE status = 'PENDING' ORDER BY created_at LIMIT 100;
  → For each: publish to Kafka
  → On success: UPDATE outbox SET status='PUBLISHED', published_at=NOW() WHERE id=?
```

**Pros**: Simple, no additional tools needed  
**Cons**: Polling adds DB load; potential latency (up to 100ms delay)

---

### B. CDC-Based Relay (Production Standard)

**Change Data Capture (CDC)** reads the database's **binary log (binlog/WAL)** instead of polling.

```
MySQL Binlog / Postgres WAL
         │
         │ Debezium (CDC Tool) reads log changes in real-time
         ▼
    ┌─────────────┐
    │   Debezium  │ ────► Publishes to Kafka Topic: "outbox.events"
    │  Connector  │
    └─────────────┘
         │
    Reads INSERT/UPDATE events on the outbox table
    → No polling, no DB load
    → Sub-millisecond latency
```

**Pros**: Near-real-time, no polling overhead, doesn't miss events  
**Cons**: Requires Debezium + Kafka Connect infrastructure

---

## 6. Idempotency — Handling Duplicate Publishing

The relay may publish the same event **more than once** (e.g., it published, but crashed before marking it as PUBLISHED). Consumers **must be idempotent**:

```java
// Consumer must handle duplicates
public void handleOrderCreated(OrderCreatedEvent event) {
    // Check if we've already processed this event
    if (processedEventRepository.existsById(event.getEventId())) {
        log.warn("Duplicate event ignored: {}", event.getEventId());
        return; // Idempotency check — skip duplicate
    }
    
    // Process the event
    paymentService.initiatePayment(event.getOrderId(), event.getAmount());
    
    // Mark as processed
    processedEventRepository.save(new ProcessedEvent(event.getEventId()));
}
```

---

## 7. Outbox vs Saga: Which Solves What?

| | Outbox | Saga |
|---|--------|------|
| **Problem** | Reliably publishing events after a DB write | Coordinating multi-step distributed transactions |
| **Scope** | Single service | Multiple services |
| **Works Together?** | ✅ Yes — Saga uses Outbox to reliably publish its events |

**They work together:**
```
Saga Step 1 (Order Service):
  BEGIN TRANSACTION
    INSERT INTO orders VALUES (...)
    INSERT INTO outbox VALUES ('OrderCreated', ...) ← Outbox pattern inside Saga!
  COMMIT
  
  Outbox Relay → publishes OrderCreatedEvent to Kafka
  
  Saga Orchestrator receives event → Triggers Step 2 (Payment Service)
```

---

## 8. Summary

```
┌────────────────────────────────────────────────────────────┐
│                Transactional Outbox Summary                 │
├───────────────────────┬────────────────────────────────────┤
│ Problem Solved        │ Dual-write (DB + Message Broker)   │
│ Core Idea             │ Write event to same DB atomically  │
│ Relay Options         │ Polling or CDC (Debezium)          │
│ Guarantee             │ At-least-once delivery             │
│ Consumer Requirement  │ Must be idempotent                 │
│ Used By               │ Stripe, LinkedIn, Eventuate.io     │
└───────────────────────┴────────────────────────────────────┘
```
