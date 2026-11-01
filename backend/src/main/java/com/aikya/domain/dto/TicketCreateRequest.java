package com.aikya.domain.dto;

public record TicketCreateRequest(
        Long unitId,
        String unitNumber,
        String category,
        String priority,
        String title,
        String description,
        String attachmentUrl
) {
}
