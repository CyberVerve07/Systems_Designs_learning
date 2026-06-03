package com.systemdesign.notification.worker;

import com.systemdesign.notification.limiter.RateLimiter;
import com.systemdesign.notification.model.NotificationEvent;
import com.systemdesign.notification.sender.ChannelSender;
import com.systemdesign.notification.sender.EmailSender;
import com.systemdesign.notification.sender.PushSender;
import com.systemdesign.notification.sender.SmsSender;

import java.util.concurrent.BlockingQueue;

public class NotificationWorker implements Runnable {
    private final int workerId;
    private final BlockingQueue<NotificationEvent> queue;
    private final RateLimiter rateLimiter;
    private final ChannelSender emailSender = new EmailSender();
    private final ChannelSender smsSender = new SmsSender();
    private final ChannelSender pushSender = new PushSender();

    private volatile boolean running = true;

    public NotificationWorker(int workerId, BlockingQueue<NotificationEvent> queue, RateLimiter rateLimiter) {
        this.workerId = workerId;
        this.queue = queue;
        this.rateLimiter = rateLimiter;
    }

    public void stop() {
        this.running = false;
    }

    @Override
    public void run() {
        System.out.println("[WORKER-" + workerId + "] Started, waiting for notifications...");
        while (running || !queue.isEmpty()) {
            try {
                // Poll from queue with a timeout to check running state periodically
                NotificationEvent event = queue.poll(500, java.util.concurrent.TimeUnit.MILLISECONDS);
                if (event == null) {
                    continue;
                }

                System.out.println("[WORKER-" + workerId + "] Picked up event: " + event.getEventId() + " for User: " + event.getUserId());

                // 1. Rate Limiting Check
                if (!rateLimiter.allowRequest(event.getUserId())) {
                    System.err.println("[RATE LIMITER] Dropped event " + event.getEventId() + " for User '" + event.getUserId() + "' (Rate Limit Exceeded)");
                    continue;
                }

                // 2. Dispatch to specific channel
                switch (event.getChannelType()) {
                    case EMAIL:
                        emailSender.send(event);
                        break;
                    case SMS:
                        smsSender.send(event);
                        break;
                    case PUSH:
                        pushSender.send(event);
                        break;
                    default:
                        System.err.println("[WORKER-" + workerId + "] Unknown channel type for event: " + event);
                }

            } catch (InterruptedException e) {
                System.out.println("[WORKER-" + workerId + "] Interrupted, shutting down...");
                break;
            } catch (Exception e) {
                System.err.println("[WORKER-" + workerId + "] Error processing event: " + e.getMessage());
            }
        }
        System.out.println("[WORKER-" + workerId + "] Stopped.");
    }
}
