package com.aikya.platform.dto;

import com.aikya.common.Role;

public record MembershipDto(
        Long id,
        Long membershipId,
        Long associationId,
        String associationName,
        String associationNameTe,
        String associationCode,
        String schemaName,
        Role role,
        String unitNumber,
        String blockName
) {
}
