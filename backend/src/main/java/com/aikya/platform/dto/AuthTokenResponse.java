package com.aikya.platform.dto;

import java.util.List;

public record AuthTokenResponse(
        String accessToken,
        String token,
        String tokenType,
        long expiresInSeconds,
        String activeTenantSchema,
        Long activeAssociationId,
        UserDto user,
        List<MembershipDto> memberships
) {
}
