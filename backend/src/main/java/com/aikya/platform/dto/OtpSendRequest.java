package com.aikya.platform.dto;

import jakarta.validation.constraints.NotBlank;

public record OtpSendRequest(
        @NotBlank(message = "Phone number is required")
        String phoneNumber
) {
}
