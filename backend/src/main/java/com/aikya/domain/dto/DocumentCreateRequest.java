package com.aikya.domain.dto;

public record DocumentCreateRequest(
        String title,
        String titleTe,
        String category,
        String financialYear,
        String fileUrl,
        Long fileSizeBytes
) {
}
