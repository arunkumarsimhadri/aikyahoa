package com.aikya.domain.dto;

import java.util.List;

public record PollOptionDto(
        Long id,
        String optionText,
        String optionTextTe,
        Integer displayOrder,
        long voteCount,
        double percentage,
        List<String> voters
) {
}
