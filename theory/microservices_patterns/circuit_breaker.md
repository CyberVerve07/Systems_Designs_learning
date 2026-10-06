# 📖 Circuit Breaker Pattern

## 1. The Cascading Failure Problem

In microservices, services call other services. When one service is slow or down, it can bring down the whole system:

```
User ──► Order Svc ──► Payment Svc (DOWN! Takes 30s to respond with timeout)
              │
              └─► Each request holds a thread for 30 seconds
              └─► 100 concurrent users = 100 threads held
              └─► Thread pool exhausted
              └─► Order Svc itself becomes unresponsive
              └─► API Gateway can't reach Order Svc
              └─► 🔥 Cascading failure — entire system DOWN
```

This is called **cascading failure** (or cascade failure / failure amplification).

---

## 2. What is the Circuit Breaker?

Inspired by electrical circuit breakers, the pattern **stops calling a failing service** after a threshold of failures, giving it time to recover.

```
States:
  CLOSED ──[failures exceed threshold]──► OPEN ──[timeout]──► HALF-OPEN
    ▲                                                              │
    └───────────────[success]──────────────────────────────────────┘
```

### State Descriptions

| State | Behavior | Transitions |
|-------|----------|------------|
| **CLOSED** (Normal) | All requests pass through to the service | → OPEN when failure rate ≥ threshold |
| **OPEN** (Tripped) | All requests fail immediately (no actual call made) | → HALF-OPEN after timeout |
| **HALF-OPEN** (Testing) | Limited requests pass through as a test probe | → CLOSED if probe succeeds; → OPEN if probe fails |

---

## 3. State Machine Deep Dive

```
                      ┌───────────────────────────────────┐
                      │          CLOSED State             │
                      │  • Requests flow normally         │
                      │  • Failure counter tracks errors  │
                      │  • If 50% of last 10 calls fail  │
                      │    → Transition to OPEN           │
                      └───────────────────────────────────┘
                                      │
                      Failure rate > 50% (threshold)
                                      │
                                      ▼
                      ┌───────────────────────────────────┐
                      │           OPEN State              │
                      │  • All calls rejected immediately  │
                      │  • Returns fallback/error fast     │
                      │  • No actual network call made!    │
                      │  • After 60 seconds (timeout)     │
                      │    → Transition to HALF-OPEN       │
                      └───────────────────────────────────┘
                                      │
                              60 second timeout
                                      │
                                      ▼
                      ┌───────────────────────────────────┐
                      │        HALF-OPEN State            │
                      │  • Allow 5 test requests through  │
                      │  • All 5 succeed → CLOSED         │
                      │  • Any fail → back to OPEN        │
                      └───────────────────────────────────┘
```

---

## 4. Fallback Strategies

When the circuit is OPEN, you need a **fallback response** instead of an error:

| Strategy | Implementation | Example |
|---------|---------------|---------|
| **Return cached data** | Serve last known good data | "Show last known inventory count" |
| **Default response** | Return a sensible default | "Return empty recommendations list" |
| **Fail fast** | Return 503 immediately | Better than waiting 30 seconds |
| **Retry with exponential backoff** | Wait longer between retries | 1s, 2s, 4s, 8s... |
| **Call alternative service** | Failover to backup | Primary DB → Replica → Cache |

```java
// Fallback Example with Resilience4j
@CircuitBreaker(name = "paymentService", fallbackMethod = "paymentFallback")
public PaymentResponse processPayment(PaymentRequest request) {
    return paymentClient.process(request); // actual call
}

public PaymentResponse paymentFallback(PaymentRequest request, Exception ex) {
    // Fallback: queue for retry later
    pendingPaymentQueue.add(request);
    return PaymentResponse.queued("Payment queued for processing");
}
```

---

## 5. Circuit Breaker Metrics

A good circuit breaker tracks:

```
Sliding Window (Count-Based):
  Last 10 requests: [✅ ✅ ❌ ❌ ❌ ❌ ❌ ❌ ✅ ❌]
  Failure rate: 70% → OPEN (threshold: 50%)

Sliding Window (Time-Based):
  Last 10 seconds:
    - 50 calls made
    - 35 failed
  Failure rate: 70% → OPEN

Metrics tracked:
  - successCount, failureCount, rejectedCount (when OPEN)
  - slowCallCount (calls taking > threshold time, e.g., > 2s)
  - lastStateTransitionTime
  - totalCallDuration
```

---

## 6. Circuit Breaker vs Retry — They Work Together

```
Request ──► Retry Logic ──► Circuit Breaker ──► Service
               │                  │
           Retry on failure    Trip if failure
           (max 3 attempts)    rate too high
           
Flow:
  1. Request fails → Retry after 1s
  2. Still fails → Retry after 2s  
  3. Still fails → Retry after 4s
  4. All retries exhausted → Circuit Breaker records failure
  5. After 5 consecutive failures → Circuit OPENS
  6. Future requests fail immediately (no retries attempted)
```

**Configure retry count thoughtfully:**
- Too few retries → transient errors not handled
- Too many retries → amplifies load on an already-struggling service (retry storm)

---

## 7. Bulkhead Pattern (Companion to Circuit Breaker)

The Bulkhead pattern isolates resources (thread pools) per service call:

```
Without Bulkhead:
  All services share one thread pool (100 threads)
  Payment Svc is slow → consumes 90 threads
  Order Svc and User Svc starved → system-wide outage

With Bulkhead:
  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
  │ Payment Pool    │  │  Order Pool      │  │  User Pool       │
  │ (30 threads)    │  │  (30 threads)    │  │  (40 threads)    │
  │                 │  │                  │  │                  │
  │ Payment Svc ❌  │  │  Order Svc ✅   │  │  User Svc ✅    │
  │ (all busy)      │  │  (still works)   │  │  (still works)   │
  └─────────────────┘  └──────────────────┘  └─────────────────┘
  Failure is ISOLATED ✅
```

---

## 8. Resilience4j (Java Industry Standard)

```yaml
# application.yml configuration
resilience4j:
  circuitbreaker:
    instances:
      paymentService:
        sliding-window-type: COUNT_BASED       # or TIME_BASED
        sliding-window-size: 10                # Last 10 requests
        failure-rate-threshold: 50             # Open at 50% failure rate
        wait-duration-in-open-state: 60s       # Wait 60s before HALF-OPEN
        permitted-number-of-calls-in-half-open-state: 5
        slow-call-duration-threshold: 2s       # Calls > 2s count as slow
        slow-call-rate-threshold: 80           # Open if 80% calls are slow
  retry:
    instances:
      paymentService:
        max-attempts: 3
        wait-duration: 1s
        exponential-backoff-multiplier: 2
```

---

## 9. Monitoring the Circuit Breaker

**Key metrics to alert on:**
```
circuit_breaker_state{name="paymentService"} = 0 (CLOSED) ✅
circuit_breaker_state{name="paymentService"} = 1 (OPEN) 🚨 Alert!
circuit_breaker_failure_rate{name="paymentService"} = 45% ⚠️ Warning
circuit_breaker_calls_rejected_total{name="paymentService"} 📊 Track
```

Dashboard should show:
- Circuit state per service
- Failure rate over time  
- Number of calls rejected (while OPEN)
- Time spent in each state

---

## 10. Summary

```
┌──────────────────────────────────────────────────────────────┐
│                 Circuit Breaker Summary                       │
├───────────────────────────┬──────────────────────────────────┤
│ Problem Solved            │ Cascading failures               │
│ States                    │ CLOSED → OPEN → HALF-OPEN        │
│ Trip Condition            │ Failure rate > threshold         │
│ Recovery                  │ Timeout + probe requests         │
│ Fallback Strategies       │ Cache, Default, Queue, Failover  │
│ Companion Patterns        │ Retry, Bulkhead, Timeout         │
│ Java Library              │ Resilience4j (replaces Hystrix)  │
│ Used By                   │ Netflix, Amazon, Microsoft       │
└───────────────────────────┴──────────────────────────────────┘
```
