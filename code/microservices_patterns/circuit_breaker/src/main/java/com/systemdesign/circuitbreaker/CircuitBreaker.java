package com.systemdesign.circuitbreaker;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * ═══════════════════════════════════════════════════════════════
 * CIRCUIT BREAKER — Custom Implementation from Scratch
 * ═══════════════════════════════════════════════════════════════
 *
 * States:
 *   CLOSED    → Normal operation. Requests flow through.
 *   OPEN      → Tripped. All requests fail fast (no actual call).
 *   HALF_OPEN → Recovery testing. Limited requests probe the service.
 *
 * Transitions:
 *   CLOSED → OPEN:      When failure rate exceeds threshold
 *   OPEN → HALF_OPEN:   After cooldown period (openStateDurationMs)
 *   HALF_OPEN → CLOSED: If probe requests all succeed
 *   HALF_OPEN → OPEN:   If any probe request fails
 */
public class CircuitBreaker {

    // ─── Configuration ─────────────────────────────────────────
    private final String name;
    private final int failureCountThreshold;    // Number of failures to trip the breaker
    private final long openStateDurationMs;      // How long to stay OPEN before HALF-OPEN
    private final int halfOpenPermittedCalls;    // How many test calls in HALF-OPEN state

    // ─── State ─────────────────────────────────────────────────
    private final AtomicReference<State> currentState;
    private final AtomicInteger failureCount;        // Consecutive failures in CLOSED state
    private final AtomicInteger successCount;        // Consecutive successes in HALF-OPEN state
    private final AtomicInteger halfOpenCallCount;   // Total calls attempted in HALF-OPEN
    private final AtomicLong openedAt;               // Timestamp when circuit was opened (ms)

    public enum State {
        CLOSED, OPEN, HALF_OPEN
    }

    /**
     * @param name                  Name of the protected service (for logging)
     * @param failureCountThreshold Number of consecutive failures to trip the breaker
     * @param openStateDurationMs   Milliseconds to stay OPEN before retrying (e.g., 60_000)
     * @param halfOpenPermittedCalls Number of test calls allowed in HALF-OPEN state
     */
    public CircuitBreaker(String name, int failureCountThreshold,
                          long openStateDurationMs, int halfOpenPermittedCalls) {
        this.name = name;
        this.failureCountThreshold = failureCountThreshold;
        this.openStateDurationMs = openStateDurationMs;
        this.halfOpenPermittedCalls = halfOpenPermittedCalls;
        this.currentState = new AtomicReference<>(State.CLOSED);
        this.failureCount = new AtomicInteger(0);
        this.successCount = new AtomicInteger(0);
        this.halfOpenCallCount = new AtomicInteger(0);
        this.openedAt = new AtomicLong(0);
    }

    /**
     * Execute a protected call through the circuit breaker.
     *
     * @param call     The remote call to execute (e.g., HTTP request to Payment Service)
     * @param fallback Fallback supplier if the circuit is OPEN or the call fails
     * @param <T>      Return type of the call
     * @return Result from the call or fallback
     * @throws CircuitBreakerOpenException if OPEN and no fallback provided
     */
    public <T> T execute(Supplier<T> call, Supplier<T> fallback) {
        State state = getEffectiveState();
        log("State: " + state);

        switch (state) {
            case OPEN:
                log("⛔ Circuit is OPEN — rejecting request fast!");
                if (fallback != null) return fallback.get();
                throw new CircuitBreakerOpenException(
                        "Circuit Breaker [" + name + "] is OPEN. Call blocked.");

            case HALF_OPEN:
                int callCount = halfOpenCallCount.incrementAndGet();
                if (callCount > halfOpenPermittedCalls) {
                    log("⛔ HALF-OPEN probe quota exhausted — blocking request");
                    if (fallback != null) return fallback.get();
                    throw new CircuitBreakerOpenException(
                            "Circuit Breaker [" + name + "] HALF-OPEN probe limit reached.");
                }
                log("🔬 HALF-OPEN — allowing probe call #" + callCount);
                return executeCall(call, fallback, true);

            case CLOSED:
            default:
                return executeCall(call, fallback, false);
        }
    }

    /**
     * Determines the effective state, handling the OPEN → HALF-OPEN timeout transition.
     */
    private State getEffectiveState() {
        if (currentState.get() == State.OPEN) {
            long openedAtMs = openedAt.get();
            if (System.currentTimeMillis() - openedAtMs >= openStateDurationMs) {
                log("⏰ Cooldown period elapsed → Transitioning OPEN → HALF_OPEN");
                transitionTo(State.HALF_OPEN);
                halfOpenCallCount.set(0);
                successCount.set(0);
            }
        }
        return currentState.get();
    }

    /**
     * Actually executes the supplier call, recording success/failure.
     */
    private <T> T executeCall(Supplier<T> call, Supplier<T> fallback, boolean isHalfOpenProbe) {
        try {
            T result = call.get();
            onSuccess(isHalfOpenProbe);
            return result;
        } catch (Exception e) {
            onFailure(isHalfOpenProbe, e);
            if (fallback != null) return fallback.get();
            throw e;
        }
    }

    /**
     * Records a successful call. In HALF-OPEN, closes the circuit after enough successes.
     */
    private synchronized void onSuccess(boolean isHalfOpenProbe) {
        if (isHalfOpenProbe) {
            int successes = successCount.incrementAndGet();
            log("✅ HALF-OPEN probe success #" + successes + "/" + halfOpenPermittedCalls);
            if (successes >= halfOpenPermittedCalls) {
                log("🟢 All probes succeeded → Transitioning HALF_OPEN → CLOSED");
                transitionTo(State.CLOSED);
                failureCount.set(0);
                successCount.set(0);
            }
        } else {
            // In CLOSED state, reset failure count on success
            if (failureCount.get() > 0) {
                failureCount.decrementAndGet();
            }
        }
    }

    /**
     * Records a failed call. In CLOSED, opens the circuit if threshold exceeded.
     * In HALF-OPEN, re-opens immediately on any failure.
     */
    private synchronized void onFailure(boolean isHalfOpenProbe, Exception e) {
        log("❌ Call failed: " + e.getMessage());
        if (isHalfOpenProbe) {
            log("🔴 HALF-OPEN probe FAILED → Transitioning back to OPEN");
            tripCircuit();
        } else {
            int failures = failureCount.incrementAndGet();
            log("Failure count: " + failures + "/" + failureCountThreshold);
            if (failures >= failureCountThreshold) {
                log("🔴 Failure threshold exceeded → Tripping circuit to OPEN");
                tripCircuit();
            }
        }
    }

    private void tripCircuit() {
        transitionTo(State.OPEN);
        openedAt.set(System.currentTimeMillis());
        halfOpenCallCount.set(0);
        successCount.set(0);
    }

    private void transitionTo(State newState) {
        State oldState = currentState.getAndSet(newState);
        if (oldState != newState) {
            System.out.printf("[CircuitBreaker:%s] 🔄 State transition: %s → %s%n",
                    name, oldState, newState);
        }
    }

    private void log(String msg) {
        System.out.printf("[CircuitBreaker:%s] %s%n", name, msg);
    }

    public State getState() { return currentState.get(); }
    public int getFailureCount() { return failureCount.get(); }
    public String getName() { return name; }

    // ─── Custom Exception ───────────────────────────────────────
    public static class CircuitBreakerOpenException extends RuntimeException {
        public CircuitBreakerOpenException(String message) {
            super(message);
        }
    }
}
