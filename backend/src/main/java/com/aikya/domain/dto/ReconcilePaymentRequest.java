package com.aikya.domain.dto;

public record ReconcilePaymentRequest(
        String decision,
        String auditNotes
) {
}
