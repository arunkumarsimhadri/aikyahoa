package com.aikya.platform.dto;

public record OtpSendResponse(
        String phoneNumber,
        long expiresInSeconds,
        String mockOtpCode
) {
}
