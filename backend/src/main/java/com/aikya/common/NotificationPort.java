package com.aikya.common;

import java.util.Map;

public interface NotificationPort {
    void sendNotification(Long recipientUserId, String phoneNumber, String recipientName,
                          String preferredLocale, String channel, String templateKey,
                          Map<String, Object> variables);
}

