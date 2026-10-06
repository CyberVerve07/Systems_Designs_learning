package com.systemdesign.circuitbreaker;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ═══════════════════════════════════════════════════════════════
 * CIRCUIT BREAKER DEMO
 * ═══════════════════════════════════════════════════════════════
 *
 * Simulates:
 *   1. Normal operation (CLOSED state)
 *   2. Payment service starts failing → Circuit OPENS
 *   3. Fast-fail during OPEN state (no actual calls made)
 *   4. Cooldown expires → HALF-OPEN probe
 *   5. Service recovers → Circuit CLOSES
 */
public class CircuitBreakerDemo {

    // Simulated external service that can fail on demand
    private static boolean paymentServiceDown = false;
    private static final AtomicInteger callCounter = new AtomicInteger(0);

    public static void main(String[] args) throws InterruptedException {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║            CIRCUIT BREAKER PATTERN DEMO                      ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");

        // Configure: trip after 3 failures, wait 3 seconds, allow 2 probes in HALF-OPEN
        CircuitBreaker cb = new CircuitBreaker("PaymentService",
                3,          // failureCountThreshold
                3000,       // openStateDurationMs (3 seconds for demo)
                2           // halfOpenPermittedCalls
        );

        // ──────────────────────────────────────────────────────────
        // Phase 1: Normal operation — all requests succeed
        // ──────────────────────────────────────────────────────────
        System.out.println("\n═══ PHASE 1: Normal Operation (service UP) ═══");
        for (int i = 1; i <= 3; i++) {
            String result = cb.execute(
                    () -> callPaymentService("order-" + callCounter.incrementAndGet()),
                    () -> "FALLBACK: Payment queued"
            );
            System.out.println("Result: " + result);
            Thread.sleep(200);
        }

        // ──────────────────────────────────────────────────────────
        // Phase 2: Service goes DOWN → failures accumulate → circuit OPENS
        // ──────────────────────────────────────────────────────────
        System.out.println("\n═══ PHASE 2: Service DOWN — Circuit should trip after 3 failures ═══");
        paymentServiceDown = true;

        for (int i = 1; i <= 5; i++) {
            try {
                String result = cb.execute(
                        () -> callPaymentService("order-" + callCounter.incrementAndGet()),
                        () -> "FALLBACK: Payment queued for retry"
                );
                System.out.println("Result: " + result);
            } catch (Exception e) {
                System.out.println("Exception caught: " + e.getMessage());
            }
            Thread.sleep(200);
        }

        // ──────────────────────────────────────────────────────────
        // Phase 3: Circuit is OPEN — all requests fail fast
        // ──────────────────────────────────────────────────────────
        System.out.println("\n═══ PHASE 3: Circuit OPEN — Fast-fail (no actual service calls!) ═══");
        System.out.println("Circuit State: " + cb.getState());
        for (int i = 1; i <= 3; i++) {
            String result = cb.execute(
                    () -> callPaymentService("order-" + callCounter.incrementAndGet()),
                    () -> "FALLBACK: Payment queued (circuit open)"
            );
            System.out.println("Result: " + result + "  [NOTE: Payment service was NOT called!]");
        }

        // ──────────────────────────────────────────────────────────
        // Phase 4: Wait for cooldown → HALF-OPEN probe
        // ──────────────────────────────────────────────────────────
        System.out.println("\n═══ PHASE 4: Waiting 3s cooldown → HALF-OPEN probe ═══");
        Thread.sleep(3100); // Wait slightly longer than openStateDurationMs

        // Service comes back up
        paymentServiceDown = false;
        System.out.println("Service is back UP. Circuit should probe...");

        for (int i = 1; i <= 3; i++) {
            String result = cb.execute(
                    () -> callPaymentService("order-" + callCounter.incrementAndGet()),
                    () -> "FALLBACK: Payment queued"
            );
            System.out.println("Result: " + result);
            Thread.sleep(200);
        }

        // ──────────────────────────────────────────────────────────
        // Phase 5: Circuit should be CLOSED again
        // ──────────────────────────────────────────────────────────
        System.out.println("\n═══ PHASE 5: Normal Operation Resumed ═══");
        System.out.println("Circuit State: " + cb.getState());
        for (int i = 1; i <= 3; i++) {
            String result = cb.execute(
                    () -> callPaymentService("order-" + callCounter.incrementAndGet()),
                    () -> "FALLBACK: Payment queued"
            );
            System.out.println("Result: " + result);
        }

        System.out.println("\n✅ Demo complete. Final state: " + cb.getState());
    }

    /**
     * Simulates calling an external payment service.
     * Throws an exception if the service is "down".
     */
    private static String callPaymentService(String orderId) {
        if (paymentServiceDown) {
            throw new RuntimeException("Connection refused: Payment Service unavailable");
        }
        // Simulate network latency
        try { Thread.sleep(50); } catch (InterruptedException ignored) {}
        return "Payment processed successfully for " + orderId;
    }
}
