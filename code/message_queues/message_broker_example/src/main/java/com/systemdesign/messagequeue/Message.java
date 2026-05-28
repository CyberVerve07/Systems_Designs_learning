package com.systemdesign.messagequeue;

import java.util.UUID;

/**
 * Represents a Message payload flowing through our broker.
 */
public class Message {
    private final String id;
    private final String payload;
    private final String destination;
    private final long timestamp;
    private int retryCount;
    private long lastAttemptTimestamp;

    public Message(String payload, String destination) {
        this.id = UUID.randomUUID().toString();
        this.payload = payload;
        this.destination = destination;
        this.timestamp = System.currentTimeMillis();
        this.retryCount = 0;
        this.lastAttemptTimestamp = 0;
    }

    public String getId() {
        return id;
    }

    public String getPayload() {
        return payload;
    }

    public String getDestination() {
        return destination;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    public long getLastAttemptTimestamp() {
        return lastAttemptTimestamp;
    }

    public void setLastAttemptTimestamp(long lastAttemptTimestamp) {
        this.lastAttemptTimestamp = lastAttemptTimestamp;
    }

    @Override
    public String toString() {
        return String.format("Message{id='%s', payload='%s', destination='%s', retries=%d}", 
                id.substring(0, 8), payload, destination, retryCount);
    }
}
