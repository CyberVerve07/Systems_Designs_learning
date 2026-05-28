package com.systemdesign.messagequeue;

/**
 * Tracks a message currently being processed by a consumer.
 * Used to implement acknowledgment timeout verification.
 */
public class InFlightMessage {
    private final Message message;
    private final Consumer consumer;
    private final long sentTime;
    private final long timeoutMs;

    public InFlightMessage(Message message, Consumer consumer, long timeoutMs) {
        this.message = message;
        this.consumer = consumer;
        this.sentTime = System.currentTimeMillis();
        this.timeoutMs = timeoutMs;
    }

    public Message getMessage() {
        return message;
    }

    public Consumer getConsumer() {
        return consumer;
    }

    public long getSentTime() {
        return sentTime;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    /**
     * Checks if the processing time has exceeded the allotted timeout.
     */
    public boolean isTimedOut() {
        return (System.currentTimeMillis() - sentTime) > timeoutMs;
    }
}
