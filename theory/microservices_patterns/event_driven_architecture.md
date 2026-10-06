# 📖 Event-Driven Architecture (EDA)

## 1. What is Event-Driven Architecture?

> In EDA, services communicate by producing and consuming **events** — facts that something happened in the past.

Instead of Service A directly calling Service B (tight coupling), Service A emits an event to a **message broker**, and any interested service can consume it.

```
❌ Traditional: Direct Coupling (Synchronous)
   Order Svc ──HTTP──► Payment Svc ──HTTP──► Inventory Svc ──HTTP──► Notification Svc
   (Order Svc must know about ALL downstream services!)
   (If any service is down, the entire chain breaks!)

✅ Event-Driven: Loose Coupling (Asynchronous)
   Order Svc ──event──► [Kafka] ◄── Payment Svc (subscribes)
                                ◄── Inventory Svc (subscribes)
                                ◄── Notification Svc (subscribes)
   (Order Svc only knows about the Event Bus — nothing else!)
```

---

## 2. Three Models of EDA

### A. Event Notification
Services notify others that something happened. **Minimal data in the event**.

```json
// Minimal event — just a notification
{
  "eventType": "OrderPlaced",
  "orderId": "uuid-123",
  "timestamp": "2024-01-15T10:30:00Z"
}
// Consumers must call back to fetch order details
// Pros: Events are small | Cons: Extra HTTP call needed
```

### B. Event-Carried State Transfer (ECST)
Events carry **all the data** needed. Consumers are fully self-sufficient.

```json
{
  "eventType": "OrderPlaced",
  "orderId": "uuid-123",
  "customerId": "cust-456",
  "items": [{ "productId": "prod-789", "qty": 2, "price": 49.99 }],
  "total": 99.98,
  "shippingAddress": { "street": "123 Main St", ... },
  "timestamp": "2024-01-15T10:30:00Z"
}
// Consumers have everything they need — no callback required
// Pros: Decoupled | Cons: Large events, schema coupling
```

### C. Event Sourcing
The **event log is the system of record**. See the CQRS doc for details.

---

## 3. Anatomy of a Good Event

```
┌─────────────────────────────────────────────────────────┐
│                      Event Schema                        │
├───────────────────────┬─────────────────────────────────┤
│  eventId              │ UUID (for idempotency)           │
│  eventType            │ "OrderPlaced" (past tense noun!) │
│  aggregateType        │ "Order"                          │
│  aggregateId          │ "order-uuid-123"                 │
│  version              │ 1 (for schema evolution)         │
│  timestamp            │ ISO-8601 UTC                     │
│  payload              │ Event-specific data              │
│  correlationId        │ Trace across services            │
│  causationId          │ ID of event that caused this one │
└───────────────────────┴─────────────────────────────────┘
```

**Naming Convention**: Always use **past tense** — events describe facts that happened:
- ✅ `OrderPlaced`, `PaymentProcessed`, `InventoryReserved`
- ❌ `PlaceOrder`, `ProcessPayment`, `ReserveInventory` (those are commands!)

---

## 4. Message Broker Deep Dive: Kafka vs RabbitMQ

| Feature | Apache Kafka | RabbitMQ |
|---------|-------------|---------|
| **Model** | Log-based (pull) | Queue-based (push) |
| **Message Retention** | Configurable (days/forever) | Deleted after consumption |
| **Replay** | ✅ Yes (rewind offset) | ❌ No |
| **Ordering** | Per partition | Per queue |
| **Throughput** | Millions/sec | Hundreds of thousands/sec |
| **Consumers** | Consumer Groups | Competing Consumers |
| **Best For** | Event streaming, CQRS, Audit log | Task queues, work distribution |

**Kafka Architecture:**
```
         Producer                    Consumer Group A
  Order Svc ──────►  ┌──────────────────────────────────────┐
  Payment Svc ────►  │           Kafka Cluster              │
                     │                                      │
                     │  Topic: "orders"                     │
                     │  ┌──────────┬──────────┬──────────┐  │ ──► Payment Svc
                     │  │Partition0│Partition1│Partition2│  │ ──► Inventory Svc
                     │  │ [msg1]   │ [msg2]   │ [msg3]   │  │
                     │  │ [msg4]   │ [msg5]   │ [msg6]   │  │
                     │  └──────────┴──────────┴──────────┘  │
                     │  Offset: 4         Offset: 5         │
                     └──────────────────────────────────────┘
```

---

## 5. Consumer Groups and Competing Consumers

```
Topic: "orders" (3 partitions)

Consumer Group: "payment-service" (3 instances)
  - Instance 1 → reads Partition 0
  - Instance 2 → reads Partition 1
  - Instance 3 → reads Partition 2
  
  Result: Each message is processed by EXACTLY ONE instance (parallelism!)

Consumer Group: "notification-service" (1 instance)
  - Instance 1 → reads ALL 3 partitions
  
  Result: All messages processed by the same instance (bottleneck at scale)
```

---

## 6. Handling Failures: Dead Letter Queue (DLQ)

When a consumer fails to process an event (after retries), it goes to the **Dead Letter Queue**:

```
Normal Flow:
  Kafka Topic "orders" ──► Consumer ──► Process Event ──► ACK ✅

Failure Flow:
  Kafka Topic "orders" ──► Consumer ──► Process Event ──► FAILS
                                            │
                                     Retry 3 times
                                            │
                                     Still fails
                                            ▼
                              Kafka Topic "orders.DLQ"
                                            │
                              ┌─────────────▼──────────────┐
                              │  DLQ Processor / Alert     │
                              │  - Notify on-call engineer │
                              │  - Manual replay option    │
                              └────────────────────────────┘
```

---

## 7. Event Ordering Guarantees

Kafka guarantees ordering **within a partition**. Use the same partition key for related events:

```
Partition Key = OrderId

Partition 0:
  [OrderCreated, orderId=A] → [OrderPaid, orderId=A] → [OrderShipped, orderId=A]
  Guaranteed Order ✅

Partition 1:
  [OrderCreated, orderId=B] → [OrderPaid, orderId=B] → [OrderShipped, orderId=B]

Problem: Messages from different partitions can interleave:
  P0: OrderCreated(A) → OrderPaid(A)
  P1:             OrderCreated(B)
  P0:                         OrderShipped(A) ← Still in order for A ✅
```

---

## 8. Event Schema Evolution

Events are contracts. Services must handle **old and new versions**:

```
// v1 Event (2023)
{ "type": "OrderPlaced", "orderId": "...", "amount": 99.99 }

// v2 Event (2024) — Added "currency" field
{ "type": "OrderPlaced", "orderId": "...", "amount": 99.99, "currency": "USD" }
```

**Rules for backward-compatible schema evolution:**
1. **Always add optional fields** — never remove required fields
2. **Add defaults** for new fields (old consumers won't break)
3. Use **Avro/Protobuf** with a Schema Registry for strict enforcement
4. Embed a **version number** in every event

---

## 9. EDA Patterns Summary

```
┌─────────────────────────────────────────────────────────────┐
│             Event-Driven Architecture Summary                │
├──────────────────────────┬──────────────────────────────────┤
│ Event Notification       │ Small events, consumers call back │
│ Event-Carried State      │ Full data in event, self-contained│
│ Event Sourcing           │ Event log is the source of truth  │
│ Message Broker           │ Kafka (streaming) / RabbitMQ (Q) │
│ Ordering                 │ Guaranteed per partition key      │
│ Failure Handling         │ Retry + Dead Letter Queue (DLQ)  │
│ Schema Evolution         │ Add optional fields + versioning  │
│ Key Benefit              │ Decoupling + Async Processing     │
│ Key Challenge            │ Eventual consistency, DLQ mgmt   │
└──────────────────────────┴──────────────────────────────────┘
```
