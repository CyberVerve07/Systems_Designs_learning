package com.systemdesign.ratelimiter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe In-Memory implementation of the Sliding Window Counter Algorithm.
 * Combines low memory footprints with high accuracy by tracking two adjacent window counters.
 */
public class SlidingWindowCounterRateLimiter implements RateLimiter {
    
    private final int limit;
    private final long windowSizeMs; // Window size in milliseconds (e.g., 60,000 for 1 minute)
    
    private final Map<String, ClientWindow> clientWindows = new ConcurrentHashMap<>();

    public SlidingWindowCounterRateLimiter(int limit, long windowSizeMs) {
        this.limit = limit;
        this.windowSizeMs = windowSizeMs;
    }

    @Override
    public boolean allowRequest(String clientId) {
        ClientWindow window = clientWindows.computeIfAbsent(clientId, k -> new ClientWindow());
        return window.allow(System.currentTimeMillis());
    }

    @Override
    public String getStrategyName() {
        return "Sliding Window Counter (In-Memory)";
    }

    private class ClientWindow {
        private long currentWindowId = 0;
        private int currentCount = 0;
        private int previousCount = 0;

        public synchronized boolean allow(long now) {
            long windowId = now / windowSizeMs;
            
            // If the window has changed:
            if (windowId != currentWindowId) {
                // If it's the immediate next window, slide the previous count
                if (windowId == currentWindowId + 1) {
                    previousCount = currentCount;
                } else {
                    // Time has jumped ahead by more than 1 window, reset previous count
                    previousCount = 0;
                }
                currentCount = 0;
                currentWindowId = windowId;
            }

            // Calculate percentage overlap in current window
            double timeIntoCurrentWindow = (double) (now % windowSizeMs);
            double overlapPercentage = timeIntoCurrentWindow / windowSizeMs;

            // Mathematical formula to approximate sliding window requests
            double estimatedRequests = (previousCount * (1 - overlapPercentage)) + currentCount;

            if (estimatedRequests < limit) {
                currentCount++;
                return true;
            }
            
            return false;
        }
    }
}
