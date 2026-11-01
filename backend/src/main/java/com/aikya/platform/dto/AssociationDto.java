package com.aikya.platform.dto;

import com.aikya.common.BillingType;

import java.math.BigDecimal;
import java.time.Instant;

public record AssociationDto(
        Long id,
        String name,
        String nameTe,
        String code,
        String schemaName,
        String address,
        String city,
        String state,
        String pincode,
        BillingType billingType,
        BigDecimal ratePerSqft,
        BigDecimal flatMaintenanceAmount,
        BigDecimal sinkingFundAmount,
        BigDecimal waterChargePerUnit,
        BigDecimal lateFeePercentage,
        boolean active,
        Instant createdAt
) {
}
