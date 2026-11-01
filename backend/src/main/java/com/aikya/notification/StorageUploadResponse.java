package com.aikya.notification;

import java.time.Instant;

public record StorageUploadResponse(
        String url,
        String fileUrl,
        String fileName,
        String category,
        String entityType,
        String contentType,
        String storageProvider,
        String storagePath,
        String storedPath,
        String tenantSchema,
        long sizeBytes,
        long fileSizeBytes,
        Instant uploadedAt,
        Instant createdAt
) {
}
