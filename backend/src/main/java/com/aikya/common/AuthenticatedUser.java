package com.aikya.common;

public record AuthenticatedUser(
        Long userId,
        String phoneNumber,
        String fullName,
        String preferredLocale,
        Long associationId,
        String tenantSchema,
        Role role,
        String unitNumber,
        String blockName
) {
}

