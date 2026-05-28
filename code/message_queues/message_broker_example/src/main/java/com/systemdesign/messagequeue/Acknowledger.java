package com.systemdesign.messagequeue;

/**
 * Interface given to a consumer to acknowledge the receipt and successful processing
 * of a message, or negatively acknowledge (fail) it.
 */
public interface Acknowledger {
    /**
     * Acknowledges the status of the message processing.
     * 
     * @param success true if processed successfully (ACK), false if failed (NACK)
     */
    void ack(boolean success);
}
