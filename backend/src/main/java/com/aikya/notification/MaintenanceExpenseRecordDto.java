package com.aikya.notification;

import java.time.Instant;
import java.util.List;

public record MaintenanceExpenseRecordDto(
        Long id,
        String expenseNumber,
        String category,
        String title,
        String titleTe,
        String vendorName,
        Double amount,
        String expenseDate,
        String paymentMode,
        String transactionReference,
        List<String> receiptFileUrls,
        List<String> attachments,
        String recordedByName,
        String approvedByName,
        String status,
        String notes,
        Instant createdAt
) {
}
