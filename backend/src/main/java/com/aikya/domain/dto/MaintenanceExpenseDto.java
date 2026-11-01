package com.aikya.domain.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record MaintenanceExpenseDto(
        Long id,
        String expenseNumber,
        String category,
        String title,
        String titleTe,
        String vendorName,
        BigDecimal amount,
        LocalDate expenseDate,
        String paymentMode,
        String transactionReference,
        List<String> receiptFileUrls,
        List<String> attachments,
        Long recordedByUserId,
        String recordedByName,
        String approvedByName,
        String status,
        String notes,
        Instant createdAt
) {
}
