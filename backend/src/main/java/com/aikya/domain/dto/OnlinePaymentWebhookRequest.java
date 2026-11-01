package com.aikya.domain.dto;

import java.math.BigDecimal;

public record OnlinePaymentWebhookRequest(
        Long invoiceId,
        String gatewayOrderId,
        String gatewayPaymentId,
        String gatewayName,
        BigDecimal amount
) {
}
