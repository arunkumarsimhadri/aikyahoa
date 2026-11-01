package com.aikya.domain.dto;

import java.time.Instant;
import java.util.List;

public record PollCreateRequest(
        String title,
        String titleTe,
        String description,
        String descriptionTe,
        boolean isSecret,
        boolean ownerOnly,
        Instant endsAt,
        List<PollOptionInput> options
) {
    public record PollOptionInput(
            String optionText,
            String optionTextTe
    ) {
    }
}
