package com.systemdesign.notification.sender;

import com.systemdesign.notification.model.NotificationEvent;

public class EmailSender implements ChannelSender {
    @Override
    public void send(NotificationEvent event) throws Exception {
        // Simulate network latency (e.g. SMTP handshake and sending)
        Thread.sleep(150);
        System.out.println("[EMAIL SENDER] Successfully sent email to user '" + event.getUserId() + "': " + event.getMessage());
    }
}
