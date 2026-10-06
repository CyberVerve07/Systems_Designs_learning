package com.systemdesign.ratelimiter;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ═══════════════════════════════════════════════════════════════
 * SLIDING WINDOW LOG RATE LIMITER
 * ═══════════════════════════════════════════════════════════════
 *
 * Algorithm:
 *   - Maintain a sorted log (timestamp list) of all requests per user
 *   - When new request arrives:
 *       1. Remove all timestamps older than (now - windowSizeMs)
 *       2. Count remaining timestamps
 *       3. If count < limit → allow request, append current timestamp
 *       4. If count >= limit → deny request (429)
 *
 * ✅ PROS:
 *   - 100% accurate — completely eliminates the boundary spike problem
 *   - Enforces exact rate within any rolling time window
 *
 * ❌ CONS:
 *   - High memory usage: stores a timestamp for every single request
 *   - In production: typically stored in Redis Sorted Sets (ZSET)
 *     → ZADD userId currentTime requestId
 *     → ZREMRANGEBYSCORE userId 0 (currentTime - windowMs)
 *     → ZCARD userId
 *
 * Best for: High-security, low-volume APIs where accuracy is critical
 */
public class SlidingWindowLogRateLimiter {

    // Per-user log of request timestamps (milliseconds)
    private final Map<String, Deque<Long>> requestLogs;
    private final int maxRequests;
    private final long windowSizeMs;

    /**
     * @param maxRequests  Maximum requests allowed in the sliding window
     * @param windowSizeMs Rolling window size in milliseconds (e.g., 60_000 for 1 min)
     */
    public SlidingWindowLogRateLimiter(int maxRequests, long windowSizeMs) {
        this.maxRequests = maxRequests;
        this.windowSizeMs = windowSizeMs;
        this.requestLogs = new ConcurrentHashMap<>();
    }

    /**
     * Checks if user is within rate limits using the exact sliding window log.
     *
     * Time Complexity: O(n) where n = number of requests in the log
     * Space Complexity: O(n) — stores all timestamps
     *
     * @param userId Unique user/client identifier
     * @return true if allowed, false if rate limited
     */
    public synchronized boolean allowRequest(String userId) {
        long now = System.currentTimeMillis();
        long windowStart = now - windowSizeMs;

        // Get or create the log for this user
        Deque<Long> log = requestLogs.computeIfAbsent(userId, k -> new LinkedList<>());

        // Step 1: Evict all timestamps outside the current sliding window
        while (!log.isEmpty() && log.peekFirst() <= windowStart) {
            log.pollFirst();
        }

        // Step 2: Count remaining requests in the window
        int currentCount = log.size();

        if (currentCount < maxRequests) {
            // Step 3: Allow — add current timestamp to the log
            log.addLast(now);
            System.out.printf("[SlidingWindowLog] ✅ User '%s' — Request %d/%d allowed. Window=[%dms ago → now]%n",
                    userId, currentCount + 1, maxRequests, windowSizeMs);
            return true;
        } else {
            // Step 4: Reject — limit exceeded
            long oldestInWindow = log.peekFirst();
            long retryAfterMs = (oldestInWindow + windowSizeMs) - now;
            System.out.printf("[SlidingWindowLog] ❌ User '%s' — Rate Limited! %d/%d. Retry after: %dms%n",
                    userId, currentCount, maxRequests, retryAfterMs);
            return false;
        }
    }

    /**
     * Returns milliseconds until the oldest request expires from the window.
     * This is the "Retry-After" header value.
     */
    public long getRetryAfterMs(String userId) {
        Deque<Long> log = requestLogs.get(userId);
        if (log == null || log.isEmpty()) return 0;
        long oldest = log.peekFirst();
        return Math.max(0, (oldest + windowSizeMs) - System.currentTimeMillis());
    }

    /**
     * Returns current number of requests in the sliding window for a user.
     */
    public int getCurrentWindowCount(String userId) {
        Deque<Long> log = requestLogs.get(userId);
        return log == null ? 0 : log.size();
    }
}
