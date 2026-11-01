package com.aikya.domain.dto;

import java.math.BigDecimal;

public record OnlineOrderResponse(
        String orderId,
        String gateway,
        BigDecimal amount,
        String currency,
        Long invoiceId,
        String invoiceNumber
) {
}
