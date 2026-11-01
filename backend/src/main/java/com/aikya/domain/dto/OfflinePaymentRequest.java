package com.aikya.domain.dto;

import java.math.BigDecimal;
import java.util.List;

public record OfflinePaymentRequest(
        Long invoiceId,
        BigDecimal amount,
        String paymentMethod,
        String referenceNumber,
        String proofFileUrl,
        List<String> proofFileUrls,
        List<String> attachments,
        String notes
) {
    public OfflinePaymentRequest(
            Long invoiceId,
            BigDecimal amount,
            String paymentMethod,
            String referenceNumber,
            String proofFileUrl,
            String notes
    ) {
        this(invoiceId, amount, paymentMethod, referenceNumber, proofFileUrl,
                proofFileUrl != null && !proofFileUrl.isBlank() ? List.of(proofFileUrl) : List.of(),
                proofFileUrl != null && !proofFileUrl.isBlank() ? List.of(proofFileUrl) : List.of(),
                notes);
    }

    public OfflinePaymentRequest(
            Long invoiceId,
            BigDecimal amount,
            String paymentMethod,
            String referenceNumber,
            String proofFileUrl,
            List<String> proofFileUrls,
            String notes
    ) {
        this(invoiceId, amount, paymentMethod, referenceNumber, proofFileUrl,
                proofFileUrls, proofFileUrls, notes);
    }
}
