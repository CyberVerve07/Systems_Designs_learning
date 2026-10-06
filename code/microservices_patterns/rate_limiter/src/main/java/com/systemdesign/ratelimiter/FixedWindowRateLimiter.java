package com.systemdesign.ratelimiter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ═══════════════════════════════════════════════════════════════
 * FIXED WINDOW COUNTER RATE LIMITER
 * ═══════════════════════════════════════════════════════════════
 *
 * Algorithm:
 *   - Divide time into fixed windows (e.g., every 1 minute)
 *   - Each user gets a counter per window
 *   - Allow requests until counter reaches the limit
 *   - At window reset, counter drops back to 0
 *
 * ⚠️ KNOWN FLAW — Boundary Spike Attack:
 *   Limit = 100 req/min
 *   Attacker sends 100 req at 10:00:59 (end of window 1)
 *   Attacker sends 100 req at 10:01:00 (start of window 2)
 *   Result: 200 requests in 2 seconds! Limit bypassed!
 *
 * Best for: Simple, non-critical limits where approximation is acceptable
 *           Internal service limits, non-security-critical rate limiting
 */
public class FixedWindowRateLimiter {

    // Per-user window data: userId → [windowStartMs, requestCount]
    private final ConcurrentHashMap<String, WindowEntry> userWindows;
    private final int maxRequestsPerWindow;
    private final long windowSizeMs; // Window size in milliseconds

    /**
     * @param maxRequestsPerWindow Maximum requests allowed per time window
     * @param windowSizeMs         Size of each time window in milliseconds (e.g., 60_000 for 1 min)
     */
    public FixedWindowRateLimiter(int maxRequestsPerWindow, long windowSizeMs) {
        this.maxRequestsPerWindow = maxRequestsPerWindow;
        this.windowSizeMs = windowSizeMs;
        this.userWindows = new ConcurrentHashMap<>();
    }

    /**
     * Checks if the user is within rate limits.
     *
     * @param userId Unique user/client identifier (e.g., IP address, API key)
     * @return true if allowed, false if rate limited
     */
    public synchronized boolean allowRequest(String userId) {
        long now = System.currentTimeMillis();

        WindowEntry entry = userWindows.computeIfAbsent(userId,
                k -> new WindowEntry(now, new AtomicInteger(0)));

        // Check if the current window has expired
        if (now - entry.windowStartMs >= windowSizeMs) {
            // Reset the window
            entry.windowStartMs = now;
            entry.requestCount.set(0);
            System.out.printf("[FixedWindow] User '%s' — New window started. Count reset.%n", userId);
        }

        int currentCount = entry.requestCount.incrementAndGet();

        if (currentCount <= maxRequestsPerWindow) {
            System.out.printf("[FixedWindow] ✅ User '%s' — Request %d/%d allowed.%n",
                    userId, currentCount, maxRequestsPerWindow);
            return true;
        } else {
            System.out.printf("[FixedWindow] ❌ User '%s' — Rate Limited! Count: %d/%d%n",
                    userId, currentCount, maxRequestsPerWindow);
            return false;
        }
    }

    /**
     * Returns current request count for a user in the current window.
     */
    public int getCurrentCount(String userId) {
        WindowEntry entry = userWindows.get(userId);
        return entry == null ? 0 : entry.requestCount.get();
    }

    /**
     * Returns milliseconds remaining in the current window for a user.
     */
    public long getWindowResetMs(String userId) {
        WindowEntry entry = userWindows.get(userId);
        if (entry == null) return windowSizeMs;
        long elapsed = System.currentTimeMillis() - entry.windowStartMs;
        return Math.max(0, windowSizeMs - elapsed);
    }

    // Inner class to store per-user window state
    private static class WindowEntry {
        volatile long windowStartMs;
        final AtomicInteger requestCount;

        WindowEntry(long windowStartMs, AtomicInteger requestCount) {
            this.windowStartMs = windowStartMs;
            this.requestCount = requestCount;
        }
    }
}
