package com.systemdesign.messagequeue;

/**
 * Represents a registered Message Consumer in the broker.
 */
public class Consumer {
    private final String id;
    private final String groupName;
    private final MessageHandler handler;

    public Consumer(String id, String groupName, MessageHandler handler) {
        this.id = id;
        this.groupName = groupName;
        this.handler = handler;
    }

    public String getId() {
        return id;
    }

    public String getGroupName() {
        return groupName;
    }

    public MessageHandler getHandler() {
        return handler;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Consumer)) return false;
        Consumer consumer = (Consumer) o;
        return id.equals(consumer.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return String.format("Consumer{id='%s', group='%s'}", id, groupName);
    }
}
