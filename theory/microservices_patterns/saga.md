# 📖 Saga Pattern — Distributed Transactions Without 2PC

## 1. The Core Problem

In a monolith, a database transaction guarantees **ACID** properties:
```sql
BEGIN TRANSACTION;
  UPDATE accounts SET balance = balance - 100 WHERE id = 1;
  UPDATE accounts SET balance = balance + 100 WHERE id = 2;
COMMIT; -- Either BOTH happen, or NEITHER
```

In microservices, **each service owns its own database**. There's no global transaction manager:
```
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│  Order Svc   │    │ Payment Svc  │    │Inventory Svc │
│  (MySQL)     │    │  (Postgres)  │    │  (MongoDB)   │
└──────────────┘    └──────────────┘    └──────────────┘
        │                   │                   │
        No shared transaction across these!
```

**The "Place Order" flow needs all 3 to succeed:**
1. Create order record → Order Service DB
2. Deduct payment → Payment Service DB  
3. Reserve inventory → Inventory Service DB

**What if step 3 fails after steps 1 and 2 succeed?** 💥

---

## 2. Why Not 2-Phase Commit (2PC)?

**2PC** (Two-Phase Commit) is the traditional solution — a coordinator asks all participants to "prepare", then "commit":

```
Phase 1 (Prepare):   Coordinator → "Can you commit?" → All Services
Phase 2 (Commit):    All say "Yes" → Coordinator → "Commit!"
```

**Problems with 2PC:**
- **Blocking**: All services are locked until coordinator decides. One slow service = all blocked
- **Single Point of Failure**: If coordinator crashes between Phase 1 and Phase 2, all services are stuck holding locks forever
- **Latency**: Multiple round-trips across the network for every transaction
- **Not microservices-friendly**: Most modern DBs in microservices (NoSQL, SaaS) don't support distributed transactions

---

## 3. What is the Saga Pattern?

> A **Saga** is a sequence of local transactions, each publishing events or messages that trigger the next local transaction. On failure, compensating transactions undo the completed steps.

```
                ┌─────────────────────────────────────────┐
                │              SAGA Flow                   │
                └─────────────────────────────────────────┘
                
Step 1: Order Svc   → Creates ORDER (status: PENDING)
                    → Publishes: OrderCreatedEvent
                    
Step 2: Payment Svc → Receives OrderCreatedEvent
                    → Deducts payment
                    → Publishes: PaymentSucceededEvent
                    
Step 3: Inventory Svc → Receives PaymentSucceededEvent
                      → Reserves inventory
                      → Publishes: InventoryReservedEvent
                      
Step 4: Order Svc   → Receives InventoryReservedEvent
                    → Updates ORDER status to CONFIRMED
```

On **failure at Step 3** (inventory out of stock):
```
Step 3 FAILS: InventoryReservationFailedEvent
                      ↓
Step 3C (compensate): Payment Svc → Refunds payment (PaymentRefundedEvent)
                      ↓
Step 2C (compensate): Order Svc → Cancels order (OrderCancelledEvent)
```

---

## 4. Two Saga Implementations

### A. Choreography-Based Saga (Event-Driven)

Services react to events independently. No central controller.

```
Order Svc ──[OrderCreated]──► Payment Svc ──[PaymentSucceeded]──► Inventory Svc
                                                                          │
              ◄──[PaymentRefunded]── Payment Svc ◄──[InventoryFailed]────┘
              (Compensate)           (Compensate)
```

**Flow:**
- Each service subscribes to events it cares about
- On success: publish a success event
- On failure: publish a failure event, triggering upstream compensations

**Pros:**
- Simple to implement for small sagas (2-3 steps)
- Fully decoupled — services don't know about each other
- Highly resilient (no single point of failure)

**Cons:**
- Hard to track overall saga state — it's distributed across services
- Risk of **cyclic dependencies** as the number of services grows
- Difficult to add new steps or change the flow
- Testing and debugging is extremely hard ("which service failed?")

---

### B. Orchestration-Based Saga (Recommended for Production)

A central **Saga Orchestrator** (or Saga Manager) controls the flow and calls each service directly:

```
                    ┌──────────────────────┐
                    │   Saga Orchestrator  │
                    │  (Order Saga Manager)│
                    └──────────┬───────────┘
                               │
              ┌────────────────┼───────────────────┐
              ▼                ▼                   ▼
       ┌────────────┐   ┌────────────┐   ┌────────────────┐
       │ Order Svc  │   │Payment Svc │   │ Inventory Svc  │
       └────────────┘   └────────────┘   └────────────────┘
       
Flow:
  1. Orchestrator → "Create Order" → Order Svc → OK
  2. Orchestrator → "Process Payment" → Payment Svc → OK
  3. Orchestrator → "Reserve Inventory" → Inventory Svc → FAIL!
  4. Orchestrator → "Refund Payment" → Payment Svc (compensate)
  5. Orchestrator → "Cancel Order" → Order Svc (compensate)
```

**Pros:**
- Centralized visibility — easy to track saga state and debug
- Easy to add new steps or change the business flow
- Handles complex conditional logic (if A then B, else C)
- Clear ownership of the transaction

**Cons:**
- The orchestrator can become a bottleneck
- More complex to implement
- Risk of the orchestrator holding too much business logic (God Object anti-pattern)

---

## 5. Compensating Transactions Design

Compensations must be **idempotent** — they can be executed multiple times safely:

| Original Transaction | Compensating Transaction |
|---------------------|--------------------------|
| Create Order (PENDING) | Cancel Order |
| Deduct $100 from payment | Refund $100 to payment |
| Reserve 5 units of inventory | Release 5 units back to inventory |
| Send "Order Confirmed" email | Send "Order Cancelled" email |

> ⚠️ **Note**: Some operations are **not compensable**. Sending an email cannot be "unsent". Handle these by sending a correction email ("Sorry, your order was cancelled").

---

## 6. Saga State Machine

A saga is a **state machine**. Represent it explicitly:

```
                  [START]
                    │
                    ▼
             ORDER_CREATING
                    │
           ┌────────┴────────┐
       success           failure
           │                 │
           ▼                 ▼
    PAYMENT_PROCESSING   ORDER_CANCELLED
           │
    ┌──────┴──────┐
success         failure
    │               │
    ▼               ▼
INVENTORY_RESERVING  PAYMENT_REFUNDING
    │                   │
    ▼                   ▼
ORDER_CONFIRMED    ORDER_CANCELLED
```

Store the saga state in a **Saga Store** (database table):
```sql
CREATE TABLE sagas (
    saga_id     UUID PRIMARY KEY,
    saga_type   VARCHAR(50),    -- 'ORDER_SAGA'
    current_step VARCHAR(50),   -- 'INVENTORY_RESERVING'
    status      VARCHAR(20),    -- 'PENDING' | 'COMPLETED' | 'COMPENSATING' | 'FAILED'
    payload     JSONB,          -- All relevant data for the saga
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP
);
```

---

## 7. When to Use Saga

✅ **Use Saga when:**
- You have a multi-step business process spanning multiple services
- You need eventual consistency (not strict ACID across services)
- Steps can be compensated (undone) if something fails

❌ **Avoid Saga when:**
- You need strict immediate consistency (use a single service with one DB)
- Your transactions have no natural compensating action
- The number of steps is very large (10+) — consider breaking the saga up

---

## 8. Summary

```
┌─────────────────────────────────────────────────────────┐
│                    SAGA Summary                          │
├─────────────────────┬───────────────────────────────────┤
│ Problem Solved      │ Distributed transactions          │
│ Approach            │ Local txns + compensating txns    │
│ Choreography        │ Decoupled, event-driven, complex  │
│ Orchestration       │ Central control, easier to debug  │
│ Consistency Model   │ Eventual (BASE, not ACID)         │
│ Key Challenge       │ Designing idempotent compensations│
│ Used By             │ Uber, Airbnb, Netflix             │
└─────────────────────┴───────────────────────────────────┘
```
