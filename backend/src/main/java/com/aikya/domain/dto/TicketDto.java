package com.aikya.domain.dto;

import java.time.Instant;

public record TicketDto(
        Long id,
        String ticketNumber,
        Long unitId,
        String unitNumber,
        Long raisedByUserId,
        String raisedByName,
        String category,
        String priority,
        String title,
        String description,
        String attachmentUrl,
        String status,
        Long assignedToUserId,
        String assignedTechnicianName,
        String assignedTechnicianPhone,
        Instant slaDueAt,
        boolean slaBreached,
        String resolutionNotes,
        Instant resolvedAt,
        Instant closedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
