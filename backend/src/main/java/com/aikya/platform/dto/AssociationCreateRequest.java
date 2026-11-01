package com.aikya.platform.dto;

import com.aikya.common.BillingType;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record AssociationCreateRequest(
        @NotBlank(message = "Association name is required")
        String name,
        String nameTe,
        String code,
        @NotBlank(message = "Address is required")
        String address,
        String city,
        String state,
        String pincode,
        BillingType billingType,
        BigDecimal ratePerSqft,
        BigDecimal flatMaintenanceAmount,
        BigDecimal sinkingFundAmount,
        BigDecimal waterChargePerUnit,
        BigDecimal lateFeePercentage
) {
}
