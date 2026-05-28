package com.systemdesign.messagequeue;

/**
 * Functional interface implemented by message consumers to process received messages.
 */
@FunctionalInterface
public interface MessageHandler {
    /**
     * Called when a new message is received by the consumer.
     * 
     * @param message the received message
     * @param acknowledger interface to ACK or NACK the message
     */
    void onMessage(Message message, Acknowledger acknowledger);
}
