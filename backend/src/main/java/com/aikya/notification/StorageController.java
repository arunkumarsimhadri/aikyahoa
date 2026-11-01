package com.aikya.notification;

import com.aikya.common.ApiResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/storage")
public class StorageController {

    private final LocalMockS3StorageService storageService;

    public StorageController(LocalMockS3StorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<StorageUploadResponse> uploadMultipart(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "fileName", required = false) String fileName,
            @RequestParam(value = "category", required = false, defaultValue = "RECEIPT_PROOF") String category
    ) {
        StorageUploadResponse response = storageService.storeMultipart(file, fileName, category);
        return ApiResponse.ok("File uploaded to Aikya storage", response);
    }

    @PostMapping(value = "/upload", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<StorageUploadResponse> uploadJson(
            @RequestBody(required = false) StorageUploadRequest request
    ) {
        StorageUploadResponse response = storageService.storeMetadataOrBase64(request);
        return ApiResponse.ok("File uploaded to Aikya storage", response);
    }

    @PostMapping(value = "/upload-multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<List<StorageUploadResponse>> uploadMultipleMultipart(
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(value = "category", required = false, defaultValue = "RECEIPT_PROOF") String category
    ) {
        List<StorageUploadResponse> responses = storageService.storeMultipleMultipart(files, category);
        return ApiResponse.ok("Multiple files uploaded to Aikya storage", responses);
    }

    @PostMapping(value = "/upload-multiple", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<List<StorageUploadResponse>> uploadMultipleJson(
            @RequestBody(required = false) StorageUploadRequest request
    ) {
        List<StorageUploadResponse> responses = storageService.storeMultipleMetadataOrBase64(request);
        return ApiResponse.ok("Multiple files uploaded to Aikya storage", responses);
    }

    @GetMapping("/files")
    public ApiResponse<List<StorageUploadResponse>> listFiles(
            @RequestParam(value = "tenant", required = false) String tenant
    ) {
        return ApiResponse.ok(storageService.listTenantFiles(tenant));
    }

    @GetMapping("/files/{tenant}/{filename:.+}")
    public ResponseEntity<byte[]> serveFile(
            @PathVariable("tenant") String tenant,
            @PathVariable("filename") String filename
    ) {
        byte[] data = storageService.readStoredFile(tenant, filename);
        MediaType mediaType = resolveMediaType(filename);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(data);
    }

    @GetMapping("/expenses")
    public ApiResponse<List<MaintenanceExpenseRecordDto>> listExpenses() {
        return ApiResponse.ok(storageService.getExpenseRecords());
    }

    @PostMapping("/expenses")
    public ApiResponse<MaintenanceExpenseRecordDto> createExpense(
            @RequestBody MaintenanceExpenseRecordDto request
    ) {
        MaintenanceExpenseRecordDto created = storageService.addExpenseRecord(request);
        return ApiResponse.ok("Maintenance expense recorded with receipt proofs", created);
    }

    private MediaType resolveMediaType(String filename) {
        if (filename == null) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf")) {
            return MediaType.APPLICATION_PDF;
        } else if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        } else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG;
        } else if (lower.endsWith(".txt") || lower.endsWith(".csv")) {
            return MediaType.TEXT_PLAIN;
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
