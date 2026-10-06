package com.systemdesign.ratelimiter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ═══════════════════════════════════════════════════════════════
 * RATE LIMITER FACADE / FACTORY
 * ═══════════════════════════════════════════════════════════════
 *
 * A per-user rate limiter that wraps any of the 5 algorithms.
 * Simulates a production API Gateway rate limiter:
 *   - Each user gets their own rate limiter instance
 *   - Returns HTTP headers alongside the allow/deny decision
 *   - Supports pluggable algorithm selection
 */
public class RateLimiterFacade {

    public enum Algorithm {
        TOKEN_BUCKET,
        LEAKY_BUCKET,
        FIXED_WINDOW,
        SLIDING_WINDOW_LOG,
        SLIDING_WINDOW_COUNTER
    }

    private final Map<String, Object> userLimiters = new ConcurrentHashMap<>();
    private final int maxRequests;
    private final long windowMs;
    private final Algorithm algorithm;

    public RateLimiterFacade(int maxRequests, long windowMs, Algorithm algorithm) {
        this.maxRequests = maxRequests;
        this.windowMs = windowMs;
        this.algorithm = algorithm;
    }

    /**
     * Main entry point — checks if a request from userId should be allowed.
     *
     * @param userId The unique identifier for the caller (API key, IP, user ID)
     * @return RateLimitResult with decision + HTTP headers
     */
    public RateLimitResult checkLimit(String userId) {
        switch (algorithm) {
            case TOKEN_BUCKET: {
                TokenBucketRateLimiter limiter = (TokenBucketRateLimiter)
                        userLimiters.computeIfAbsent(userId,
                                k -> new TokenBucketRateLimiter(maxRequests, maxRequests / (windowMs / 1000)));
                boolean allowed = limiter.allowRequest();
                return new RateLimitResult(allowed, maxRequests,
                        (int) limiter.getAvailableTokens(), 0L);
            }
            case FIXED_WINDOW: {
                FixedWindowRateLimiter limiter = (FixedWindowRateLimiter)
                        userLimiters.computeIfAbsent("shared_fixed_window",
                                k -> new FixedWindowRateLimiter(maxRequests, windowMs));
                boolean allowed = limiter.allowRequest(userId);
                return new RateLimitResult(allowed, maxRequests,
                        Math.max(0, maxRequests - limiter.getCurrentCount(userId)),
                        limiter.getWindowResetMs(userId));
            }
            case SLIDING_WINDOW_LOG: {
                SlidingWindowLogRateLimiter limiter = (SlidingWindowLogRateLimiter)
                        userLimiters.computeIfAbsent("shared_swl",
                                k -> new SlidingWindowLogRateLimiter(maxRequests, windowMs));
                boolean allowed = limiter.allowRequest(userId);
                return new RateLimitResult(allowed, maxRequests,
                        Math.max(0, maxRequests - limiter.getCurrentWindowCount(userId)),
                        allowed ? 0 : limiter.getRetryAfterMs(userId));
            }
            case SLIDING_WINDOW_COUNTER: {
                SlidingWindowCounterRateLimiter limiter = (SlidingWindowCounterRateLimiter)
                        userLimiters.computeIfAbsent("shared_swc",
                                k -> new SlidingWindowCounterRateLimiter(maxRequests, windowMs));
                boolean allowed = limiter.allowRequest(userId);
                return new RateLimitResult(allowed, maxRequests, -1, 0);
            }
            default:
                throw new IllegalStateException("Unknown algorithm: " + algorithm);
        }
    }

    /**
     * Represents the rate limiter decision along with HTTP headers to return.
     * Maps to standard rate-limiting HTTP headers:
     *   X-RateLimit-Limit:     Total requests allowed per window
     *   X-RateLimit-Remaining: Requests remaining in current window
     *   Retry-After:           Seconds to wait before retrying (only on 429)
     */
    public static class RateLimitResult {
        public final boolean allowed;
        public final int limit;
        public final int remaining;
        public final long retryAfterMs;

        public RateLimitResult(boolean allowed, int limit, int remaining, long retryAfterMs) {
            this.allowed = allowed;
            this.limit = limit;
            this.remaining = remaining;
            this.retryAfterMs = retryAfterMs;
        }

        public int httpStatus() {
            return allowed ? 200 : 429;
        }

        @Override
        public String toString() {
            if (allowed) {
                return String.format("HTTP 200 OK | X-RateLimit-Limit: %d | X-RateLimit-Remaining: %d",
                        limit, remaining);
            } else {
                return String.format("HTTP 429 Too Many Requests | Retry-After: %.1fs",
                        retryAfterMs / 1000.0);
            }
        }
    }
}
