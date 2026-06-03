package com.systemdesign.notification.sender;

import com.systemdesign.notification.model.NotificationEvent;

public class PushSender implements ChannelSender {
    @Override
    public void send(NotificationEvent event) throws Exception {
        // Simulate network latency (e.g. Firebase Cloud Messaging API call)
        Thread.sleep(80);
        System.out.println("[PUSH SENDER] Successfully sent Push Notification to user '" + event.getUserId() + "': " + event.getMessage());
    }
}
