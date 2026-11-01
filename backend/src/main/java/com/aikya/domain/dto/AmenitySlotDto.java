package com.aikya.domain.dto;

public record AmenitySlotDto(
        String startTime,
        String endTime,
        boolean available,
        String bookedByUnit
) {
}
