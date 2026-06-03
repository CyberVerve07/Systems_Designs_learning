package com.systemdesign.notification.model;

public class NotificationEvent {
    private final String eventId;
    private final String userId;
    private final String message;
    private final ChannelType channelType;
    private final long timestamp;

    public NotificationEvent(String eventId, String userId, String message, ChannelType channelType) {
        this.eventId = eventId;
        this.userId = userId;
        this.message = message;
        this.channelType = channelType;
        this.timestamp = System.currentTimeMillis();
    }

    public String getEventId() {
        return eventId;
    }

    public String getUserId() {
        return userId;
    }

    public String getMessage() {
        return message;
    }

    public ChannelType getChannelType() {
        return channelType;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "NotificationEvent{" +
                "eventId='" + eventId + '\'' +
                ", userId='" + userId + '\'' +
                ", message='" + message + '\'' +
                ", channelType=" + channelType +
                '}';
    }
}
