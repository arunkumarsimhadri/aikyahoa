package com.aikya.domain.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record AmenityBookingDto(
        Long id,
        Long amenityId,
        String amenityName,
        String amenityNameTe,
        Long unitId,
        String unitNumber,
        Long bookedByUserId,
        String bookedByName,
        LocalDate bookingDate,
        String startTime,
        String endTime,
        String purpose,
        Integer guestCount,
        BigDecimal feeAmount,
        String status,
        Instant createdAt
) {
}
