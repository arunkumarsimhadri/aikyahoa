package com.aikya.platform.dto;

import jakarta.validation.constraints.NotBlank;

public record OtpVerifyRequest(
        @NotBlank(message = "Phone number is required")
        String phoneNumber,
        String otpCode,
        String otp,
        Long associationId
) {
    public OtpVerifyRequest(String phoneNumber, String otpCode, Long associationId) {
        this(phoneNumber, otpCode, otpCode, associationId);
    }

    public String resolvedOtpCode() {
        if (otpCode != null && !otpCode.isBlank()) {
            return otpCode.trim();
        }
        if (otp != null && !otp.isBlank()) {
            return otp.trim();
        }
        return "";
    }
}
