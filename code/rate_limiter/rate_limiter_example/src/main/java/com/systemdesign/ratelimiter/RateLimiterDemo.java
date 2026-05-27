package com.systemdesign.ratelimiter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RateLimiterDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=================================================");
        System.out.println("   🚀 SYSTEM DESIGN: RATE LIMITER DEMO 🚀");
        System.out.println("=================================================\n");

        // 1. DEMO 1: Token Bucket Rate Limiter
        // Max capacity: 5 tokens, refills at 2 tokens per second
        RateLimiter tokenBucket = new TokenBucketRateLimiter(5, 2);
        runLimiterDemo(tokenBucket, "client-123", 10, 100);

        System.out.println("\n--- Waiting 2 seconds for Token Bucket to refill... ---\n");
        Thread.sleep(2000);
        
        // Secondary short burst to demonstrate refilled tokens
        System.out.println("--- Starting second burst on Token Bucket ---");
        runLimiterDemo(tokenBucket, "client-123", 4, 100);

        Thread.sleep(1000);

        // 2. DEMO 2: Sliding Window Counter Rate Limiter
        // Limit: 5 requests per 2 seconds window
        System.out.println("\n=================================================");
        RateLimiter slidingWindow = new SlidingWindowCounterRateLimiter(5, 2000);
        runLimiterDemo(slidingWindow, "client-456", 10, 150);

        Thread.sleep(1000);

        // 3. DEMO 3: Redis Distributed Rate Limiter
        // Limit: 3 requests per 5 seconds window
        System.out.println("\n=================================================");
        RateLimiter redisLimiter = new RedisRateLimiter(3, 5, "127.0.0.1", 6379);
        runLimiterDemo(redisLimiter, "client-789", 8, 200);

        System.out.println("\n=================================================");
        System.out.println("✅ Rate Limiter demonstration finished successfully!");
        System.out.println("=================================================");
    }

    private static void runLimiterDemo(RateLimiter limiter, String clientId, int totalRequests, int intervalMs) throws InterruptedException {
        System.out.println("Using Strategy: " + limiter.getStrategyName());
        System.out.println("Simulating " + totalRequests + " requests from '" + clientId + "' at " + intervalMs + "ms intervals:\n");

        ExecutorService executor = Executors.newFixedThreadPool(3);

        for (int i = 1; i <= totalRequests; i++) {
            final int requestId = i;
            executor.submit(() -> {
                boolean allowed = limiter.allowRequest(clientId);
                if (allowed) {
                    System.out.printf("  [REQ #%02d] - ✅ 200 OK (Processed)%n", requestId);
                } else {
                    System.out.printf("  [REQ #%02d] - ❌ 429 Too Many Requests (Blocked)%n", requestId);
                }
            });
            Thread.sleep(intervalMs);
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }
}
