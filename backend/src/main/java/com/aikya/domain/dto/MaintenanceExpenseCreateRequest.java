package com.aikya.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MaintenanceExpenseCreateRequest(
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
        String approvedByName,
        String notes
) {
}
