# 🏗️ Microservices Patterns — System Design Deep Dive

> A comprehensive guide to battle-tested microservices patterns used at scale in production systems.

---

## 📚 Patterns Covered

| # | Pattern | Key Problem Solved | Industry Usage |
|---|---------|-------------------|----------------|
| 1 | [CQRS](./cqrs.md) | Read/Write scalability mismatch | Netflix, Amazon, Uber |
| 2 | [Saga](./saga.md) | Distributed transactions without 2PC | Uber Eats, Airbnb |
| 3 | [Transactional Outbox](./outbox_transaction.md) | Dual-write consistency problem | Stripe, LinkedIn |
| 4 | [Event-Driven Architecture](./event_driven_architecture.md) | Decoupling + async processing | Twitter, Shopify |
| 5 | [Circuit Breaker](./circuit_breaker.md) | Cascading failure prevention | Netflix Hystrix, Resilience4j |

---

## 🧠 Why These Patterns?

When you break a monolith into microservices, **you trade simplicity for scalability** — but this comes with hard problems:

```
❌ Monolith Problems (That pushed us to microservices)
├── A single bug can bring down the entire system
├── Cannot scale individual components independently
├── Deployments require full system downtime
└── Technology lock-in (must use one language/DB)

✅ Microservices Introduce NEW Problems
├── How do you do transactions across 3 databases? → Saga
├── How do you keep events & DB in sync? → Outbox Pattern
├── How do you separate read/write workloads? → CQRS
├── How do you handle service failures gracefully? → Circuit Breaker
└── How do services communicate without tight coupling? → Event-Driven
```

---

## 🔗 How These Patterns Work Together

```
                    ┌─────────────────────────────────┐
                    │         API Gateway              │
                    └────────────┬────────────────────┘
                                 │
              ┌──────────────────┼──────────────────┐
              ▼                  ▼                  ▼
       ┌─────────────┐   ┌─────────────┐   ┌─────────────┐
       │  Order Svc  │   │ Payment Svc │   │Inventory Svc│
       │  (Write DB) │   │             │   │             │
       └──────┬──────┘   └──────┬──────┘   └──────┬──────┘
              │                 │                  │
     ┌────────▼──────┐         │                  │
     │  Outbox Table │         │                  │
     │(Transactional)│         │                  │
     └────────┬──────┘         │                  │
              │ CDC Relay       │                  │
              ▼                 ▼                  ▼
       ┌────────────────────────────────────────────────┐
       │              Event Bus (Kafka)                 │
       │         Event-Driven Architecture              │
       └──────────────┬─────────────────┬──────────────┘
                      │                 │
             ┌────────▼──────┐  ┌───────▼────────┐
             │  Read Model   │  │  Saga Orches-  │
             │  (CQRS Query) │  │  trator/Chorus │
             └───────────────┘  └────────────────┘
                                        │
                                Circuit Breaker wraps
                                all inter-service calls
```

---

## 📖 Reading Order (Recommended)

1. Start with **Event-Driven Architecture** — it's the foundation
2. Then **CQRS** — builds on event streams for read/write separation  
3. Then **Outbox Transaction** — solves the "how do I reliably publish events?" question
4. Then **Saga** — solves distributed transactions using events
5. Finally **Circuit Breaker** — wraps all inter-service calls for resilience

---

*See `../../code/microservices_patterns/` for working Java implementations of all patterns.*
