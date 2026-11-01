package com.aikya.common;

import java.math.BigDecimal;

public interface AssociationConfigLookupPort {

    record AssociationBillingConfig(
            Long associationId,
            String name,
            String code,
            String schemaName,
            BillingType billingType,
            BigDecimal ratePerSqft,
            BigDecimal flatMaintenanceAmount,
            BigDecimal sinkingFundAmount,
            BigDecimal waterChargePerUnit,
            BigDecimal lateFeePercentage
    ) {
    }

    AssociationBillingConfig getCurrentAssociationConfig(String schemaName);
}

