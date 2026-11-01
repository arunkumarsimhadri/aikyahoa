package com.aikya.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BillingRunRequest(
        Integer billingMonth,
        Integer billingYear,
        LocalDate dueDate,
        BigDecimal otherCharges,
        String notes
) {
}
