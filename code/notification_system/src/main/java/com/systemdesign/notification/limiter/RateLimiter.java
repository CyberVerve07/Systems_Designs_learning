package com.systemdesign.notification.limiter;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe Token Bucket Rate Limiter.
 * Restricts the number of notifications a user can receive in a given period.
 */
public class RateLimiter {
    private final int maxTokens;
    private final long refillIntervalMs;
    private final int refillTokens;
    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public RateLimiter(int maxTokens, long refillIntervalMs, int refillTokens) {
        this.maxTokens = maxTokens;
        this.refillIntervalMs = refillIntervalMs;
        this.refillTokens = refillTokens;
    }

    /**
     * Tries to consume a token for a given user.
     * @param userId Unique identifier of the user
     * @return true if allowed, false if rate limited
     */
    public boolean allowRequest(String userId) {
        TokenBucket bucket = buckets.computeIfAbsent(userId, k -> new TokenBucket(maxTokens, refillIntervalMs, refillTokens));
        return bucket.tryConsume();
    }

    private static class TokenBucket {
        private final int maxCapacity;
        private final long refillIntervalMs;
        private final int refillTokens;
        
        private double tokens;
        private long lastRefillTime;

        public TokenBucket(int maxCapacity, long refillIntervalMs, int refillTokens) {
            this.maxCapacity = maxCapacity;
            this.refillIntervalMs = refillIntervalMs;
            this.refillTokens = refillTokens;
            this.tokens = maxCapacity;
            this.lastRefillTime = System.currentTimeMillis();
        }

        public synchronized boolean tryConsume() {
            refill();
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long timePassed = now - lastRefillTime;
            
            if (timePassed >= refillIntervalMs) {
                double tokensToAdd = ((double) timePassed / refillIntervalMs) * refillTokens;
                tokens = Math.min(maxCapacity, tokens + tokensToAdd);
                lastRefillTime = now;
            }
        }
    }
}
