package com.aikya.domain.dto;

public record OnlineOrderRequest(
        Long invoiceId,
        String gateway
) {
}
