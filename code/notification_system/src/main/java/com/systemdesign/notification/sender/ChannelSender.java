package com.systemdesign.notification.sender;

import com.systemdesign.notification.model.NotificationEvent;

public interface ChannelSender {
    void send(NotificationEvent event) throws Exception;
}
