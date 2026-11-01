package com.aikya.domain.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PaymentDto(
        Long id,
        Long invoiceId,
        String invoiceNumber,
        Long unitId,
        String unitNumber,
        Long payerUserId,
        String payerName,
        BigDecimal amount,
        String paymentMethod,
        String gatewayName,
        String gatewayOrderId,
        String gatewayPaymentId,
        String referenceNumber,
        String proofFileUrl,
        List<String> proofFileUrls,
        List<String> attachments,
        String reconciliationStatus,
        Long reconciledByUserId,
        String reconciledByName,
        Instant reconciledAt,
        String auditNotes,
        Instant paidAt,
        Instant createdAt
) {
}
