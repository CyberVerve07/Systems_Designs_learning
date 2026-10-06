# 📖 CQRS — Command Query Responsibility Segregation

## 1. The Core Idea

> **"Reads and Writes are fundamentally different workloads. Stop forcing them to share the same model."**

CQRS separates your application into two halves:

| Side | Responsibility | Optimized For |
|------|---------------|---------------|
| **Command** | Writes (Create, Update, Delete) | Consistency, Validation, Business Rules |
| **Query** | Reads (GET requests, searches) | Performance, Denormalized views, Caching |

---

## 2. The Problem CQRS Solves

### Without CQRS (Traditional Model)
```
                    ┌─────────────┐
  GET /orders  ───► │             │ ───► SELECT * FROM orders
  POST /order  ───► │  OrderSvc   │ ───► INSERT INTO orders
  GET /report  ───► │  (1 Model)  │ ───► SELECT + JOIN 5 tables (SLOW!)
                    └─────────────┘
                          │
                    ┌─────────────┐
                    │  One DB     │ ← Write + Read contention
                    │  (MySQL)    │ ← Complex JOINs hurt writes
                    └─────────────┘
```

**Problems:**
- Read queries (complex JOINs, aggregations) slow down the write path
- Adding read indexes degrades write performance
- Can't scale reads independently from writes (reads are 90%+ of traffic in most apps)
- The domain model is often polluted with DTO/view logic

### With CQRS
```
     Commands (Writes)                 Queries (Reads)
  POST /order ──────►  ┌──────────┐    GET /orders ──────► ┌──────────┐
  PUT /order  ──────►  │ Command  │                         │  Query   │
  DELETE /ord ──────►  │ Handler  │                         │ Handler  │
                       └────┬─────┘                         └────┬─────┘
                            │ Writes                             │ Reads
                            ▼                                    ▼
                    ┌───────────────┐                 ┌──────────────────┐
                    │  Write Store  │ ──(sync/async)► │   Read Store     │
                    │  (MySQL,      │   Domain Events  │  (Redis, ES,     │
                    │   Postgres)   │                  │   Mongo, etc.)   │
                    └───────────────┘                 └──────────────────┘
```

---

## 3. CQRS with Event Sourcing (Power Combo)

Instead of storing *current state*, store the **sequence of events** that led to that state:

```
Traditional State Storage:
  orders table: { id: 1, status: "SHIPPED", total: 99.99 }

Event Sourcing:
  events table:
    { id: 1, orderId: 1, type: "ORDER_CREATED",  data: {...}, ts: T1 }
    { id: 2, orderId: 1, type: "PAYMENT_RECEIVED", data: {...}, ts: T2 }
    { id: 3, orderId: 1, type: "ORDER_SHIPPED",  data: {...}, ts: T3 }
    
  Current state = Replay all events in sequence
```

**Benefits of Event Sourcing + CQRS:**
- **Full audit trail** for free — you have the complete history
- **Time travel** — replay events to any point in time
- **Multiple read models** — build different projections from the same events
- **Eventual consistency** — read models are rebuilt asynchronously

---

## 4. Deep Dive: Command Flow

```
HTTP Request → Command Object → Command Bus → Command Handler → Domain Model → Write DB
                                                    │
                                                    └──► Publishes Domain Event
                                                              │
                                                              ▼
                                                    Event Handler → Update Read Model
```

### Step-by-step: `PlaceOrderCommand`
1. `POST /orders` arrives at controller
2. Controller creates `PlaceOrderCommand { userId, items, address }`
3. Command Bus routes it to `PlaceOrderCommandHandler`
4. Handler validates the command (inventory available? payment valid?)
5. Handler creates `Order` aggregate and saves to Write DB (MySQL)
6. Handler publishes `OrderPlacedEvent` to event bus (Kafka)
7. **Event Handler** on the query side receives `OrderPlacedEvent`
8. Read model in Elasticsearch is updated with denormalized order data
9. Next GET request instantly gets data from fast read model

---

## 5. Query Side (Read Model Design)

Read models should be **denormalized** — pre-joining everything the UI needs:

```json
// Write Model (normalized)
orders:        { id, user_id, status, created_at }
order_items:   { id, order_id, product_id, qty, price }
products:      { id, name, category }
users:         { id, name, email }

// Read Model (denormalized — what the API actually returns)
{
  "orderId": "uuid-123",
  "status": "SHIPPED",
  "customer": { "name": "Alice", "email": "alice@example.com" },
  "items": [
    { "productName": "iPhone 15", "category": "Electronics", "qty": 1, "price": 999.99 }
  ],
  "total": 999.99,
  "createdAt": "2024-01-15T10:30:00Z"
}
```

---

## 6. Eventual Consistency & Lag

The read model is **eventually consistent** with the write model. There's a lag between:
- `T1`: Order is written to MySQL
- `T2`: OrderPlacedEvent is published to Kafka
- `T3`: Event consumer updates Elasticsearch (Read Model)

```
T0        T1         T2         T3
│         │          │          │
│  Write  │ Publish  │ Consume  │ Read model
│  to DB  │  Event   │  Event   │  updated
└─────────┴──────────┴──────────┘
          ← lag →   ← lag →
          (ms)       (ms-seconds)
```

**Handling this in practice:**
- Show a "Processing..." state in UI after command submission
- Use WebSockets/SSE to push update when read model is ready
- For critical reads (payment confirmation), query the write side directly

---

## 7. When to Use CQRS

✅ **Use CQRS when:**
- Read traffic >> Write traffic (e.g., 100:1 ratio)
- Read and write performance requirements are very different
- You need multiple views of the same data (dashboard, report, search)
- Complex domain with rich business logic on writes
- Teams can be split: write team vs. read team

❌ **Avoid CQRS when:**
- Simple CRUD apps where reads and writes are balanced
- Small teams that can't afford the operational overhead
- Strict consistency is required (financial systems needing immediate read-your-writes)

---

## 8. Summary

```
┌─────────────────────────────────────────────────────┐
│                    CQRS Summary                      │
├─────────────────┬───────────────────────────────────┤
│ Commands        │ Change state, return void/ack      │
│ Queries         │ Return data, never change state    │
│ Write Store     │ Normalized, consistent, MySQL/PG   │
│ Read Store      │ Denormalized, fast, Redis/ES/Mongo │
│ Sync Mechanism  │ Domain Events via Kafka/RabbitMQ   │
│ Consistency     │ Eventual (ms to seconds lag)       │
│ Trade-off       │ Complexity for scalability         │
└─────────────────┴───────────────────────────────────┘
```
