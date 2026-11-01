package com.aikya.domain.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record InvoiceDto(
        Long id,
        String invoiceNumber,
        Long unitId,
        String unitNumber,
        String blockName,
        String ownerName,
        Integer billingMonth,
        Integer billingYear,
        LocalDate dueDate,
        BigDecimal maintenanceCharge,
        BigDecimal sinkingFundCharge,
        BigDecimal waterCharge,
        BigDecimal otherCharges,
        BigDecimal lateFee,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal balanceDue,
        String status,
        String notes,
        Instant createdAt
) {
}
