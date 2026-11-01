package com.aikya.domain.dto;

public record PollVoteRequest(
        Long optionId,
        Long unitId,
        String unitNumber
) {
}
