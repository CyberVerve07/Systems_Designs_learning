package com.systemdesign.ratelimiter;

import java.util.concurrent.TimeUnit;

/**
 * ═══════════════════════════════════════════════════════════════
 * RATE LIMITER DEMO — All 5 Algorithms
 * ═══════════════════════════════════════════════════════════════
 *
 * Demonstrates all 5 rate limiting algorithms:
 *   1. Token Bucket
 *   2. Leaky Bucket
 *   3. Fixed Window Counter
 *   4. Sliding Window Log
 *   5. Sliding Window Counter
 *
 * Run: mvn exec:java -Dexec.mainClass="com.systemdesign.ratelimiter.RateLimiterDemo"
 */
public class RateLimiterDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║            RATE LIMITER ALGORITHMS DEMO                      ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");

        demo1TokenBucket();
        Thread.sleep(500);
        System.out.println();

        demo2LeakyBucket();
        Thread.sleep(500);
        System.out.println();

        demo3FixedWindow();
        Thread.sleep(500);
        System.out.println();

        demo4SlidingWindowLog();
        Thread.sleep(500);
        System.out.println();

        demo5SlidingWindowCounter();
        Thread.sleep(500);
        System.out.println();

        demo6FacadeComparison();
    }

    // ─────────────────────────────────────────────────────────────
    // DEMO 1: Token Bucket
    // ─────────────────────────────────────────────────────────────
    private static void demo1TokenBucket() throws InterruptedException {
        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println(" DEMO 1: TOKEN BUCKET (capacity=5, refill=2 tokens/sec)");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // Bucket holds 5 tokens, refills 2 per second
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(5, 2);

        // Burst: send 7 requests instantly (only 5 should pass)
        System.out.println("\n[Phase 1] Burst of 7 requests — expect 5 allowed, 2 denied:");
        for (int i = 1; i <= 7; i++) {
            boolean allowed = limiter.allowRequest();
            System.out.printf("  Request %d: %s (tokens left: %d)%n",
                    i, allowed ? "✅ ALLOWED" : "❌ DENIED", limiter.getAvailableTokens());
        }

        // Wait for refill
        System.out.println("\n[Phase 2] Waiting 2 seconds for token refill (expect +4 tokens)...");
        Thread.sleep(2000);

        System.out.println("[Phase 2] After refill:");
        for (int i = 1; i <= 3; i++) {
            boolean allowed = limiter.allowRequest();
            System.out.printf("  Request %d: %s (tokens left: %d)%n",
                    i, allowed ? "✅ ALLOWED" : "❌ DENIED", limiter.getAvailableTokens());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // DEMO 2: Leaky Bucket
    // ─────────────────────────────────────────────────────────────
    private static void demo2LeakyBucket() throws InterruptedException {
        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println(" DEMO 2: LEAKY BUCKET (capacity=3, leak=500ms/req)");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // Queue holds 3, processes one every 500ms
        LeakyBucketRateLimiter limiter = new LeakyBucketRateLimiter(3, 500);

        System.out.println("\n[Phase 1] Submit 5 requests instantly (only 3 can queue):");
        for (int i = 1; i <= 5; i++) {
            final int reqNum = i;
            boolean queued = limiter.trySubmit(() ->
                    System.out.println("  ⚙️  Processing queued request #" + reqNum));
            if (!queued) {
                System.out.printf("  Request %d: ❌ BUCKET FULL — Dropped!%n", i);
            }
        }

        System.out.println("\n[Phase 2] Waiting 2 seconds to let bucket drain...");
        Thread.sleep(2000);
        limiter.shutdown();
    }

    // ─────────────────────────────────────────────────────────────
    // DEMO 3: Fixed Window Counter
    // ─────────────────────────────────────────────────────────────
    private static void demo3FixedWindow() {
        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println(" DEMO 3: FIXED WINDOW (limit=3 req/5 sec)");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(3, 5000);

        System.out.println("\nUser 'alice' — 5 requests:");
        for (int i = 1; i <= 5; i++) {
            limiter.allowRequest("alice");
        }

        System.out.println("\nUser 'bob' — 2 requests (different user, own counter):");
        for (int i = 1; i <= 2; i++) {
            limiter.allowRequest("bob");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // DEMO 4: Sliding Window Log
    // ─────────────────────────────────────────────────────────────
    private static void demo4SlidingWindowLog() throws InterruptedException {
        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println(" DEMO 4: SLIDING WINDOW LOG (limit=3 req/3 sec)");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        SlidingWindowLogRateLimiter limiter = new SlidingWindowLogRateLimiter(3, 3000);

        System.out.println("\n[Phase 1] 4 rapid requests (3 allowed, 1 denied):");
        for (int i = 1; i <= 4; i++) {
            limiter.allowRequest("user-1");
        }

        System.out.println("\n[Phase 2] Wait 2 seconds (oldest request expires in 1s more)...");
        Thread.sleep(2000);
        System.out.println("[Phase 2] One more request — should still be limited:");
        limiter.allowRequest("user-1");

        System.out.println("\n[Phase 3] Wait 2 more seconds (window fully cleared)...");
        Thread.sleep(2000);
        System.out.println("[Phase 3] Request after full window expiry — should be allowed:");
        limiter.allowRequest("user-1");
    }

    // ─────────────────────────────────────────────────────────────
    // DEMO 5: Sliding Window Counter
    // ─────────────────────────────────────────────────────────────
    private static void demo5SlidingWindowCounter() {
        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println(" DEMO 5: SLIDING WINDOW COUNTER (limit=5 req/min)");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        SlidingWindowCounterRateLimiter limiter = new SlidingWindowCounterRateLimiter(5, 60_000);

        System.out.println("\nSending 7 requests:");
        for (int i = 1; i <= 7; i++) {
            limiter.allowRequest("api-key-xyz");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // DEMO 6: Facade — Compare algorithms side-by-side
    // ─────────────────────────────────────────────────────────────
    private static void demo6FacadeComparison() {
        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println(" DEMO 6: FACADE — Algorithm Comparison (limit=3 req/5 sec)");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        RateLimiterFacade fixedFacade = new RateLimiterFacade(
                3, 5000, RateLimiterFacade.Algorithm.FIXED_WINDOW);
        RateLimiterFacade swlFacade = new RateLimiterFacade(
                3, 5000, RateLimiterFacade.Algorithm.SLIDING_WINDOW_LOG);

        System.out.println("\n5 requests — Fixed Window vs Sliding Window Log:");
        System.out.println("─────────────────────────────────────────────────────");
        System.out.printf("%-8s | %-45s | %-45s%n", "Request", "Fixed Window", "Sliding Window Log");
        System.out.println("─────────────────────────────────────────────────────");
        for (int i = 1; i <= 5; i++) {
            RateLimiterFacade.RateLimitResult r1 = fixedFacade.checkLimit("test-user");
            RateLimiterFacade.RateLimitResult r2 = swlFacade.checkLimit("test-user");
            System.out.printf("%-8d | %-45s | %-45s%n", i, r1.toString(), r2.toString());
        }
    }
}
