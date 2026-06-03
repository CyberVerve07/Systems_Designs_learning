package com.systemdesign.notification;

import com.systemdesign.notification.limiter.RateLimiter;
import com.systemdesign.notification.model.ChannelType;
import com.systemdesign.notification.model.NotificationEvent;
import com.systemdesign.notification.worker.NotificationWorker;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.UUID;

public class NotificationApp {
    public static void main(String[] args) {
        System.out.println("=== Starting Distributed Notification System Simulation ===");

        // 1. Thread-safe Message Queue (Simulating a broker like RabbitMQ / Kafka topic)
        LinkedBlockingQueue<NotificationEvent> messageQueue = new LinkedBlockingQueue<>();

        // 2. Rate Limiter: Max 3 notifications, refills 3 tokens every 5 seconds (5000ms)
        RateLimiter rateLimiter = new RateLimiter(3, 5000, 3);

        // 3. Worker Pool: Start 3 concurrent workers to consume events
        int workerCount = 3;
        ExecutorService executorService = Executors.newFixedThreadPool(workerCount);
        NotificationWorker[] workers = new NotificationWorker[workerCount];

        for (int i = 0; i < workerCount; i++) {
            workers[i] = new NotificationWorker(i + 1, messageQueue, rateLimiter);
            executorService.submit(workers[i]);
        }

        // 4. Simulate publishing events
        System.out.println("\n[PRODUCER] Publishing notification events...");

        // User A (Alice) spamming 5 notifications immediately
        for (int i = 1; i <= 5; i++) {
            String eventId = "evt-alice-" + i;
            ChannelType type = (i % 2 == 0) ? ChannelType.EMAIL : ChannelType.SMS;
            NotificationEvent event = new NotificationEvent(eventId, "alice", "Promo Code part " + i, type);
            messageQueue.offer(event);
        }

        // User B (Bob) sending 2 notifications
        for (int i = 1; i <= 2; i++) {
            String eventId = "evt-bob-" + i;
            NotificationEvent event = new NotificationEvent(eventId, "bob", "Welcome notification " + i, ChannelType.PUSH);
            messageQueue.offer(event);
        }

        // Give workers some time to process the first batch
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Sleep to wait for the 5-second Rate Limiter refill window to reset
        System.out.println("\n[SYSTEM] Waiting 3 seconds for rate limit tokens to refill...");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // User A (Alice) sends another notification now (should be allowed since refill happened)
        System.out.println("\n[PRODUCER] Alice sends another notification after token refill...");
        NotificationEvent refillEvent = new NotificationEvent("evt-alice-refill", "alice", "New Refill Message", ChannelType.EMAIL);
        messageQueue.offer(refillEvent);

        // Wait for all messages to finish processing
        while (!messageQueue.isEmpty()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Sleep to let workers print final output
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 5. Shutdown workers gracefully
        System.out.println("\n=== Stopping Workers and Shutting Down ===");
        for (NotificationWorker worker : workers) {
            worker.stop();
        }
        executorService.shutdown();
        System.out.println("Simulation finished.");
    }
}
