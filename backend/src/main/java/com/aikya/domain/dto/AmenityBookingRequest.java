package com.aikya.domain.dto;

import java.time.LocalDate;

public record AmenityBookingRequest(
        Long amenityId,
        Long unitId,
        String unitNumber,
        LocalDate bookingDate,
        String startTime,
        String endTime,
        String purpose,
        Integer guestCount
) {
}
