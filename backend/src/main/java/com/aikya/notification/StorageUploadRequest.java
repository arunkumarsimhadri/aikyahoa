package com.aikya.notification;

import java.util.List;

public record StorageUploadRequest(
        String fileName,
        String filename,
        String category,
        String contentType,
        String base64Data,
        Long sizeBytes,
        List<StorageUploadRequest> files
) {
    public String resolvedFileName() {
        if (fileName != null && !fileName.isBlank()) {
            return fileName;
        }
        if (filename != null && !filename.isBlank()) {
            return filename;
        }
        return "document.pdf";
    }
}
