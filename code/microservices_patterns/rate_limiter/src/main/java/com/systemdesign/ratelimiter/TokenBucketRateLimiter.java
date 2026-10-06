package com.systemdesign.ratelimiter;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ═══════════════════════════════════════════════════════════════
 * TOKEN BUCKET RATE LIMITER
 * ═══════════════════════════════════════════════════════════════
 * 
 * Algorithm:
 *   - A bucket holds up to 'capacity' tokens
 *   - Tokens are added at a fixed rate (refillRate tokens/second)
 *   - Each request consumes 1 token
 *   - If no tokens available → request is REJECTED (HTTP 429)
 *   - Allows burst traffic (if bucket is full, burst of 'capacity' requests)
 *
 * Used by: AWS API Gateway, Stripe, GitHub
 *
 * Thread Safety: AtomicLong ensures lock-free concurrency
 */
public class TokenBucketRateLimiter {

    private final long capacity;            // Max tokens the bucket can hold
    private final long refillRatePerSecond; // Tokens added per second
    private AtomicLong availableTokens;     // Current token count
    private AtomicLong lastRefillTimestamp; // Last time tokens were added (nanoseconds)

    /**
     * @param capacity           Maximum tokens in the bucket (burst limit)
     * @param refillRatePerSecond Tokens to add per second (sustained rate limit)
     */
    public TokenBucketRateLimiter(long capacity, long refillRatePerSecond) {
        this.capacity = capacity;
        this.refillRatePerSecond = refillRatePerSecond;
        this.availableTokens = new AtomicLong(capacity); // Start with a full bucket
        this.lastRefillTimestamp = new AtomicLong(System.nanoTime());
    }

    /**
     * Attempts to consume 1 token from the bucket.
     *
     * @return true if request is allowed, false if rate limited (429)
     */
    public synchronized boolean allowRequest() {
        refill(); // First, add any new tokens that have accumulated since last call

        if (availableTokens.get() > 0) {
            availableTokens.decrementAndGet();
            return true; // ✅ Request allowed
        }
        return false; // ❌ Bucket empty — rate limited
    }

    /**
     * Calculates how many new tokens to add based on elapsed time since last refill.
     * Formula: newTokens = elapsedSeconds × refillRatePerSecond
     */
    private void refill() {
        long now = System.nanoTime();
        long lastRefill = lastRefillTimestamp.get();
        long elapsedNanos = now - lastRefill;

        // Convert nanoseconds to tokens: (elapsedNanos / 1e9) * refillRatePerSecond
        long newTokens = (long) (elapsedNanos * refillRatePerSecond / 1_000_000_000.0);

        if (newTokens > 0) {
            long updatedTokens = Math.min(capacity, availableTokens.get() + newTokens);
            availableTokens.set(updatedTokens);
            lastRefillTimestamp.set(now);
        }
    }

    public long getAvailableTokens() {
        refill();
        return availableTokens.get();
    }

    @Override
    public String toString() {
        return String.format("TokenBucket[capacity=%d, rate=%d/s, available=%d]",
                capacity, refillRatePerSecond, availableTokens.get());
    }
}
