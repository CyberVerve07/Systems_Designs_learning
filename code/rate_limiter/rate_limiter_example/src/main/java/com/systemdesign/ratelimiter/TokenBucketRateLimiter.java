package com.systemdesign.ratelimiter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe, highly scalable In-Memory implementation of the Token Bucket Algorithm.
 * Uses a "lazy refill" strategy instead of dedicated background threads.
 */
public class TokenBucketRateLimiter implements RateLimiter {
    
    private final int maxBucketSize;
    private final int refillRatePerSecond;
    
    // Stores individual buckets for each client.
    private final Map<String, Bucket> clientBuckets = new ConcurrentHashMap<>();

    public TokenBucketRateLimiter(int maxBucketSize, int refillRatePerSecond) {
        this.maxBucketSize = maxBucketSize;
        this.refillRatePerSecond = refillRatePerSecond;
    }

    @Override
    public boolean allowRequest(String clientId) {
        Bucket bucket = clientBuckets.computeIfAbsent(clientId, k -> new Bucket(maxBucketSize));
        return bucket.consume();
    }

    @Override
    public String getStrategyName() {
        return "Token Bucket (In-Memory Lazy Refill)";
    }

    // Inner class representing a single client's bucket.
    private class Bucket {
        private final double maxTokens;
        private double currentTokens;
        private long lastRefillTimestamp;

        public Bucket(double maxTokens) {
            this.maxTokens = maxTokens;
            this.currentTokens = maxTokens;
            this.lastRefillTimestamp = System.currentTimeMillis();
        }

        // Synchronized to make token checks and updates thread-safe per user bucket.
        public synchronized boolean consume() {
            refill();
            
            if (currentTokens >= 1.0) {
                currentTokens -= 1.0;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long timeElapsedMs = now - lastRefillTimestamp;
            
            if (timeElapsedMs > 0) {
                // Calculate tokens to add based on elapsed time and refill rate.
                double tokensToAdd = (timeElapsedMs / 1000.0) * refillRatePerSecond;
                
                if (tokensToAdd > 0) {
                    currentTokens = Math.min(maxTokens, currentTokens + tokensToAdd);
                    lastRefillTimestamp = now;
                }
            }
        }
    }
}
