package com.aikya.platform.dto;

import com.aikya.common.Role;

public record SwitchContextRequest(
        Long associationId,
        Long membershipId,
        Long userId,
        String phoneNumber,
        Role role
) {
    public SwitchContextRequest(Long associationId, Long membershipId) {
        this(associationId, membershipId, null, null, null);
    }
}
