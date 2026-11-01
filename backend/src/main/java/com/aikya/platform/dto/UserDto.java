package com.aikya.platform.dto;

import com.aikya.common.Role;

import java.util.List;

public record UserDto(
        Long id,
        String phoneNumber,
        String fullName,
        String fullNameTe,
        String email,
        String preferredLocale,
        Role activeRole,
        Long activeAssociationId,
        String activeSchemaName,
        String activeUnitNumber,
        String activeBlockName,
        List<MembershipDto> memberships
) {
    public Role getRole() {
        return activeRole;
    }

    public String getUnitNumber() {
        return activeUnitNumber;
    }

    public String getBlockName() {
        return activeBlockName;
    }
}
