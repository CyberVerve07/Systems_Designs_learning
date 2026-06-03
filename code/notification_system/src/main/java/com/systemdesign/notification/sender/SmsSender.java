package com.systemdesign.notification.sender;

import com.systemdesign.notification.model.NotificationEvent;

public class SmsSender implements ChannelSender {
    @Override
    public void send(NotificationEvent event) throws Exception {
        // Simulate network latency (e.g. Twilio API call)
        Thread.sleep(100);
        System.out.println("[SMS SENDER] Successfully sent SMS to user '" + event.getUserId() + "': " + event.getMessage());
    }
}
