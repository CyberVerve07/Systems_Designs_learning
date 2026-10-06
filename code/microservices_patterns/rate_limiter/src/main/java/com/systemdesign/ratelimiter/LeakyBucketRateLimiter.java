package com.systemdesign.ratelimiter;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ═══════════════════════════════════════════════════════════════
 * LEAKY BUCKET RATE LIMITER
 * ═══════════════════════════════════════════════════════════════
 *
 * Algorithm:
 *   - Requests enter a fixed-size FIFO queue (the "bucket")
 *   - A background scheduler "leaks" (processes) one request at a fixed rate
 *   - If the queue is full when a request arrives → REJECTED immediately
 *   - Guarantees a perfectly SMOOTH output rate (no bursts downstream)
 *
 * Best for: Traffic shaping — protecting downstream services from burst load
 *           Background job queues, SMS/Email pipelines
 *
 * Difference from Token Bucket:
 *   - Token Bucket ALLOWS burst (bucket can fill up and drain fast)
 *   - Leaky Bucket SMOOTHS burst (processes at constant rate regardless)
 */
public class LeakyBucketRateLimiter {

    private final BlockingQueue<Runnable> queue; // The "bucket" (bounded queue)
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean running;
    private final int capacity;                  // Max queue size
    private final long leakRateMs;               // Process one request every N milliseconds

    /**
     * @param capacity    Maximum requests that can be queued
     * @param leakRateMs  How often (in ms) one request is processed (e.g., 100ms = 10 req/sec)
     */
    public LeakyBucketRateLimiter(int capacity, long leakRateMs) {
        this.capacity = capacity;
        this.leakRateMs = leakRateMs;
        this.queue = new ArrayBlockingQueue<>(capacity);
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "leaky-bucket-processor");
            t.setDaemon(true);
            return t;
        });
        this.running = new AtomicBoolean(true);

        // Start the "leak" — process one queued request every leakRateMs milliseconds
        scheduler.scheduleAtFixedRate(this::processNext, 0, leakRateMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Attempts to queue a request for processing.
     *
     * @param request The work to do when this request is processed
     * @return true if queued (will be processed), false if bucket is full (429)
     */
    public boolean trySubmit(Runnable request) {
        if (queue.offer(request)) {
            System.out.println("[LeakyBucket] ✅ Request queued. Queue size: " + queue.size() + "/" + capacity);
            return true;  // ✅ Queued
        } else {
            System.out.println("[LeakyBucket] ❌ Bucket FULL. Request DROPPED! Queue: " + queue.size() + "/" + capacity);
            return false; // ❌ Bucket full — rate limited
        }
    }

    /**
     * "Leaks" one request — called by the scheduler at a fixed rate.
     * This is what gives the leaky bucket its smooth, constant output rate.
     */
    private void processNext() {
        Runnable request = queue.poll();
        if (request != null) {
            try {
                System.out.println("[LeakyBucket] ⚙️ Processing queued request... Queue remaining: " + queue.size());
                request.run();
            } catch (Exception e) {
                System.err.println("[LeakyBucket] Error processing request: " + e.getMessage());
            }
        }
    }

    public int getQueueSize() {
        return queue.size();
    }

    public void shutdown() {
        running.set(false);
        scheduler.shutdown();
        System.out.println("[LeakyBucket] Scheduler shut down.");
    }

    @Override
    public String toString() {
        return String.format("LeakyBucket[capacity=%d, leakRate=%dms, queued=%d]",
                capacity, leakRateMs, queue.size());
    }
}
