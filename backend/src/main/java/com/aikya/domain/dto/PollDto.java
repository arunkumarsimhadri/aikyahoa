package com.aikya.domain.dto;

import java.time.Instant;
import java.util.List;

public record PollDto(
        Long id,
        String title,
        String titleTe,
        String description,
        String descriptionTe,
        boolean isSecret,
        boolean ownerOnly,
        String status,
        Instant startsAt,
        Instant endsAt,
        String createdByName,
        long totalVotes,
        boolean currentUserUnitVoted,
        Long votedOptionId,
        List<PollOptionDto> options,
        Instant createdAt
) {
}
