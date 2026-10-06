package com.systemdesign.ratelimiter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ═══════════════════════════════════════════════════════════════
 * SLIDING WINDOW COUNTER RATE LIMITER
 * ═══════════════════════════════════════════════════════════════
 *
 * Algorithm (Best of both worlds):
 *   - Uses only 2 counters per user: previousWindowCount + currentWindowCount
 *   - Approximates the rolling window count using a weighted formula:
 *
 *   estimatedCount = (prevCount × overlapFraction) + currentCount
 *
 *   where overlapFraction = (windowSize - timeElapsedInCurrentWindow) / windowSize
 *
 * Example (limit=100 req/min):
 *   Previous window: 80 requests
 *   Current window: 30 requests, 30 seconds elapsed (50% through window)
 *   overlapFraction = (60s - 30s) / 60s = 0.5
 *   estimatedCount = (80 × 0.5) + 30 = 40 + 30 = 70 → ALLOWED ✅
 *
 * ✅ PROS:
 *   - Extremely low memory: only 2 integers per user (vs. thousands of timestamps)
 *   - Solves the boundary spike problem with ~99%+ accuracy
 *   - Highly performant — O(1) time and O(1) space per request
 *
 * ❌ CONS:
 *   - Slight approximation (not 100% mathematically exact)
 *   - Assumes requests are uniformly distributed in the previous window
 *
 * Best for: High-scale, memory-sensitive systems like public APIs
 *           Used by: Nginx, Kong, Cloudflare
 */
public class SlidingWindowCounterRateLimiter {

    private final Map<String, UserWindowState> userStates;
    private final int maxRequests;
    private final long windowSizeMs;

    /**
     * @param maxRequests  Maximum requests allowed per rolling window
     * @param windowSizeMs Rolling window size in milliseconds
     */
    public SlidingWindowCounterRateLimiter(int maxRequests, long windowSizeMs) {
        this.maxRequests = maxRequests;
        this.windowSizeMs = windowSizeMs;
        this.userStates = new ConcurrentHashMap<>();
    }

    /**
     * Checks if user is within rate limits using sliding window approximation.
     *
     * Time Complexity: O(1)
     * Space Complexity: O(1) — only 2 counters stored
     *
     * @param userId Unique user/client identifier
     * @return true if allowed, false if rate limited
     */
    public synchronized boolean allowRequest(String userId) {
        long now = System.currentTimeMillis();
        long currentWindowStart = (now / windowSizeMs) * windowSizeMs; // Floor to window boundary

        UserWindowState state = userStates.computeIfAbsent(userId,
                k -> new UserWindowState(currentWindowStart));

        // Detect if we've rolled into a new window
        if (currentWindowStart > state.currentWindowStart) {
            // Roll: current → previous, reset current
            state.prevWindowCount = state.currentWindowCount.get();
            state.prevWindowStart = state.currentWindowStart;
            state.currentWindowCount.set(0);
            state.currentWindowStart = currentWindowStart;
        } else if (currentWindowStart > state.prevWindowStart + windowSizeMs) {
            // Previous window is now expired — no overlap
            state.prevWindowCount = 0;
        }

        // Calculate the overlap fraction of the previous window
        long timeInCurrentWindow = now - currentWindowStart;
        double overlapFraction = 1.0 - ((double) timeInCurrentWindow / windowSizeMs);

        // Estimated requests in rolling window
        double estimatedCount = (state.prevWindowCount * overlapFraction)
                + state.currentWindowCount.get();

        System.out.printf("[SlidingWindowCounter] User '%s' — prevCount=%d, currCount=%d, " +
                        "overlap=%.2f, estimated=%.2f, limit=%d%n",
                userId, state.prevWindowCount, state.currentWindowCount.get(),
                overlapFraction, estimatedCount, maxRequests);

        if (estimatedCount < maxRequests) {
            state.currentWindowCount.incrementAndGet();
            System.out.printf("[SlidingWindowCounter] ✅ Request ALLOWED. Estimated: %.1f/%d%n",
                    estimatedCount + 1, maxRequests);
            return true;
        } else {
            System.out.printf("[SlidingWindowCounter] ❌ Rate Limited! Estimated: %.1f/%d%n",
                    estimatedCount, maxRequests);
            return false;
        }
    }

    // Per-user state: only 4 fields — very memory efficient!
    private static class UserWindowState {
        long currentWindowStart;     // When the current window started
        long prevWindowStart;        // When the previous window started
        int prevWindowCount;         // Request count in previous window
        AtomicInteger currentWindowCount; // Request count in current window

        UserWindowState(long currentWindowStart) {
            this.currentWindowStart = currentWindowStart;
            this.prevWindowStart = currentWindowStart - 1; // dummy
            this.prevWindowCount = 0;
            this.currentWindowCount = new AtomicInteger(0);
        }
    }
}
