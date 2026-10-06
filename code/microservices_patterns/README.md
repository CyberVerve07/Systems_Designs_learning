# 💻 Microservices Patterns — Code Implementations

Practical Java implementations of all 5 core microservices patterns.

## 📂 Module Structure

```
microservices_patterns/
├── pom.xml                    ← Parent multi-module Maven project
├── rate_limiter/              ← All 5 rate limiting algorithms
│   └── src/main/java/com/systemdesign/ratelimiter/
│       ├── TokenBucketRateLimiter.java
│       ├── LeakyBucketRateLimiter.java
│       ├── FixedWindowRateLimiter.java
│       ├── SlidingWindowLogRateLimiter.java
│       ├── SlidingWindowCounterRateLimiter.java
│       ├── RateLimiterFacade.java          ← Unified API for all algorithms
│       └── RateLimiterDemo.java            ← Run this!
├── circuit_breaker/           ← Custom 3-state Circuit Breaker
│   └── src/main/java/com/systemdesign/circuitbreaker/
│       ├── CircuitBreaker.java             ← Full CLOSED/OPEN/HALF-OPEN FSM
│       └── CircuitBreakerDemo.java         ← Run this!
├── cqrs/                      ← Command/Query separation
│   └── src/main/java/com/systemdesign/cqrs/
│       └── CQRSDemo.java                   ← Run this!
├── saga/                      ← Orchestration-based Saga
│   └── src/main/java/com/systemdesign/saga/
│       └── SagaDemo.java                   ← Run this!
├── outbox/                    ← Transactional Outbox pattern
│   └── src/main/java/com/systemdesign/outbox/
│       └── OutboxDemo.java                 ← Run this!
└── event_driven/              ← Event-Driven with consumer groups + DLQ
    └── src/main/java/com/systemdesign/eventdriven/
        └── EventDrivenDemo.java            ← Run this!
```

---

## 🚀 Running Each Demo

### Prerequisites
- Java 11+
- Maven 3.6+

### Run All
```bash
mvn compile
```

### Run Rate Limiter (all 5 algorithms)
```bash
cd rate_limiter
mvn compile exec:java -Dexec.mainClass="com.systemdesign.ratelimiter.RateLimiterDemo"
```

### Run Circuit Breaker
```bash
cd circuit_breaker
mvn compile exec:java -Dexec.mainClass="com.systemdesign.circuitbreaker.CircuitBreakerDemo"
```

### Run CQRS
```bash
cd cqrs
mvn compile exec:java -Dexec.mainClass="com.systemdesign.cqrs.CQRSDemo"
```

### Run Saga
```bash
cd saga
mvn compile exec:java -Dexec.mainClass="com.systemdesign.saga.SagaDemo"
```

### Run Outbox
```bash
cd outbox
mvn compile exec:java -Dexec.mainClass="com.systemdesign.outbox.OutboxDemo"
```

### Run Event-Driven
```bash
cd event_driven
mvn compile exec:java -Dexec.mainClass="com.systemdesign.eventdriven.EventDrivenDemo"
```

---

## 📖 Key Design Decisions

### Rate Limiter
| Algorithm | Key Feature | When to Use |
|-----------|------------|-------------|
| Token Bucket | Allows bursts | Standard APIs (GitHub, Stripe) |
| Leaky Bucket | Smooth output | Traffic shaping, background jobs |
| Fixed Window | Simple, fast | Non-critical limits |
| Sliding Window Log | 100% accurate | High-security, low-volume |
| Sliding Window Counter | O(1) space, ~99% accurate | High-scale public APIs |

### Circuit Breaker States
```
CLOSED ──[failures ≥ threshold]──► OPEN ──[timeout]──► HALF_OPEN
  ▲                                                          │
  └────────────────[all probes succeed]─────────────────────┘
```

### Saga vs 2PC
- **2PC**: All-or-nothing, blocking, SPOF
- **Saga**: Eventual consistency, non-blocking, compensating transactions

### Outbox Guarantee
- Order is saved **AND** event is queued in a **single DB transaction**
- Relay provides **at-least-once** delivery
- Consumers **must be idempotent** (check `eventId` before processing)

---

*See `../../theory/microservices_patterns/` for in-depth system design notes.*
