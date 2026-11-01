package com.aikya.domain.dto;

import java.time.Instant;

public record NoticeCreateRequest(
        String title,
        String titleTe,
        String content,
        String contentTe,
        String category,
        String priority,
        boolean pinned,
        Instant expiresAt
) {
}
