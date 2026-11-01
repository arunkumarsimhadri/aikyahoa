package com.aikya.domain.dto;

public record TicketStatusUpdateRequest(
        String status,
        Long assignedToUserId,
        String assignedTechnicianName,
        String assignedTechnicianPhone,
        String resolutionNotes
) {
}
