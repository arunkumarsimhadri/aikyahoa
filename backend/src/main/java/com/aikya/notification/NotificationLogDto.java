package com.aikya.notification;

import java.time.Instant;

public record NotificationLogDto(
        Long id,
        Long recipientUserId,
        String recipientPhone,
        String recipientName,
        String channel,
        String templateKey,
        String locale,
        String renderedMessage,
        String status,
        Instant createdAt
) {
    public static NotificationLogDto fromEntity(NotificationLog entity) {
        return new NotificationLogDto(
                entity.getId(),
                entity.getRecipientUserId(),
                entity.getRecipientPhone(),
                entity.getRecipientName(),
                entity.getChannel(),
                entity.getTemplateKey(),
                entity.getLocale(),
                entity.getRenderedMessage(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}
