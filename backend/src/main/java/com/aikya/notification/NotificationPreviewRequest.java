package com.aikya.notification;

import java.util.Map;

public record NotificationPreviewRequest(
        String templateKey,
        String locale,
        String channel,
        String recipientPhone,
        String recipientName,
        Map<String, Object> variables,
        Map<String, Object> parameters
) {
    public Map<String, Object> resolvedVariables() {
        if (variables != null && !variables.isEmpty()) {
            return variables;
        }
        return parameters != null ? parameters : Map.of();
    }
}
