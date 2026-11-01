package com.aikya.notification;

import com.aikya.common.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

@Service
public class LocalMockS3StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalMockS3StorageService.class);
    private static final DateTimeFormatter AMZ_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
    private static final DateTimeFormatter DATE_STAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final Path rootStorageDir;
    private final String minioEndpoint;
    private final String minioBucket;
    private final String minioAccessKey;
    private final String minioSecretKey;
    private final String minioRegion;
    private final HttpClient httpClient;
    private final CopyOnWriteArrayList<MaintenanceExpenseRecordDto> expenseRecords = new CopyOnWriteArrayList<>();

    public LocalMockS3StorageService(
            @Value("${aikya.storage.local-dir:/tmp/aikya-uploads}") String localDir,
            @Value("${aikya.storage.minio.endpoint:http://192.168.10.83}") String minioEndpoint,
            @Value("${aikya.storage.minio.bucket:aikya-storage}") String minioBucket,
            @Value("${aikya.storage.minio.access-key:admin}") String minioAccessKey,
            @Value("${aikya.storage.minio.secret-key:hxfCnBm21oiRMSoQ72FhzCOxwzjjV840}") String minioSecretKey,
            @Value("${aikya.storage.minio.region:us-east-1}") String minioRegion) {
        this.rootStorageDir = Paths.get(localDir);
        this.minioEndpoint = minioEndpoint.endsWith("/")
                ? minioEndpoint.substring(0, minioEndpoint.length() - 1)
                : minioEndpoint;
        this.minioBucket = minioBucket;
        this.minioAccessKey = minioAccessKey;
        this.minioSecretKey = minioSecretKey;
        this.minioRegion = minioRegion;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(1500))
                .build();
        try {
            Files.createDirectories(this.rootStorageDir);
        } catch (IOException ex) {
            log.warn("Could not initialize local fallback storage directory {}: {}", localDir, ex.getMessage());
        }
        seedDefaultEliteTowersExpenses();
    }

    public StorageUploadResponse storeMultipart(MultipartFile file, String customFileName, String category) {
        String originalName = (customFileName != null && !customFileName.isBlank())
                ? customFileName
                : (file != null && file.getOriginalFilename() != null ? file.getOriginalFilename() : "receipt-proof.pdf");
        String contentType = (file != null && file.getContentType() != null)
                ? file.getContentType()
                : inferContentType(originalName);
        byte[] content = new byte[0];
        if (file != null && !file.isEmpty()) {
            try {
                content = file.getBytes();
            } catch (IOException ex) {
                log.warn("Failed reading multipart bytes: {}", ex.getMessage());
            }
        } else {
            content = ("Receipt/Proof attachment: " + originalName).getBytes(StandardCharsets.UTF_8);
        }
        return writeAndBuildResponse(originalName, category, contentType, content, content.length, true);
    }

    public List<StorageUploadResponse> storeMultipleMultipart(MultipartFile[] files, String category) {
        if (files == null || files.length == 0) {
            return Collections.emptyList();
        }
        List<StorageUploadResponse> results = new ArrayList<>(files.length);
        for (MultipartFile file : files) {
            results.add(storeMultipart(file, null, category));
        }
        return results;
    }

    public StorageUploadResponse storeMetadataOrBase64(StorageUploadRequest request) {
        String rawName = request != null ? request.resolvedFileName() : "proof-upload.png";
        String category = (request != null && request.category() != null && !request.category().isBlank())
                ? request.category()
                : "MAINTENANCE_PAYMENT";
        String contentType = (request != null && request.contentType() != null && !request.contentType().isBlank())
                ? request.contentType()
                : inferContentType(rawName);
        byte[] bytes = ("Receipt/Proof payload for " + rawName).getBytes(StandardCharsets.UTF_8);
        if (request != null && request.base64Data() != null && !request.base64Data().isBlank()) {
            try {
                String cleanBase64 = request.base64Data();
                int commaIdx = cleanBase64.indexOf(',');
                if (commaIdx >= 0) {
                    cleanBase64 = cleanBase64.substring(commaIdx + 1);
                }
                bytes = Base64.getDecoder().decode(cleanBase64);
            } catch (IllegalArgumentException ignored) {
                bytes = request.base64Data().getBytes(StandardCharsets.UTF_8);
            }
        }
        long reportedSize = (request != null && request.sizeBytes() != null && request.sizeBytes() > 0)
                ? request.sizeBytes()
                : bytes.length;
        return writeAndBuildResponse(rawName, category, contentType, bytes, reportedSize, true);
    }

    public List<StorageUploadResponse> storeMultipleMetadataOrBase64(StorageUploadRequest batchRequest) {
        if (batchRequest == null) {
            return Collections.emptyList();
        }
        if (batchRequest.files() != null && !batchRequest.files().isEmpty()) {
            List<StorageUploadResponse> list = new ArrayList<>(batchRequest.files().size());
            for (StorageUploadRequest item : batchRequest.files()) {
                String cat = (item.category() != null && !item.category().isBlank())
                        ? item.category()
                        : batchRequest.category();
                StorageUploadRequest merged = new StorageUploadRequest(
                        item.fileName(),
                        item.filename(),
                        cat,
                        item.contentType(),
                        item.base64Data(),
                        item.sizeBytes(),
                        null
                );
                list.add(storeMetadataOrBase64(merged));
            }
            return list;
        }
        return List.of(storeMetadataOrBase64(batchRequest));
    }

    public byte[] readStoredFile(String tenant, String filename) {
        String safeTenant = sanitizeSegment(tenant);
        String safeFile = sanitizeFileName(filename);
        Path target = rootStorageDir.resolve(safeTenant).resolve(safeFile);
        if (Files.exists(target)) {
            try {
                return Files.readAllBytes(target);
            } catch (IOException ex) {
                log.warn("Could not read file {}: {}", target, ex.getMessage());
            }
        }
        return ("Aikya Stored Receipt/Document (" + safeTenant + "/" + safeFile + ") - Elite Towers, Ongole")
                .getBytes(StandardCharsets.UTF_8);
    }

    public List<StorageUploadResponse> listTenantFiles(String tenant) {
        String resolvedTenant = (tenant == null || tenant.isBlank()) ? TenantContext.getCurrentTenant() : tenant;
        String safeTenant = sanitizeSegment(resolvedTenant);
        Path tenantDir = rootStorageDir.resolve(safeTenant);
        List<StorageUploadResponse> results = new ArrayList<>();
        if (Files.exists(tenantDir) && Files.isDirectory(tenantDir)) {
            try (Stream<Path> paths = Files.list(tenantDir)) {
                paths.filter(Files::isRegularFile).forEach(path -> {
                    String name = path.getFileName().toString();
                    long size = 1024L;
                    Instant modified = Instant.now();
                    try {
                        size = Files.size(path);
                        modified = Files.getLastModifiedTime(path).toInstant();
                    } catch (IOException ignored) {
                    }
                    String objectKey = safeTenant + "/" + name;
                    String minioUrl = minioEndpoint + "/" + minioBucket + "/" + objectKey;
                    results.add(new StorageUploadResponse(
                            minioUrl,
                            minioUrl,
                            name,
                            "RECEIPT_PROOF",
                            "ASSOCIATION_EXPENSE",
                            inferContentType(name),
                            "MINIO_S3",
                            objectKey,
                            path.toAbsolutePath().toString(),
                            safeTenant,
                            size,
                            size,
                            modified,
                            modified
                    ));
                });
            } catch (IOException ex) {
                log.debug("Could not list directory {}: {}", tenantDir, ex.getMessage());
            }
        }
        return results;
    }

    public List<MaintenanceExpenseRecordDto> getExpenseRecords() {
        return new ArrayList<>(expenseRecords);
    }

    public MaintenanceExpenseRecordDto addExpenseRecord(MaintenanceExpenseRecordDto request) {
        long nextId = expenseRecords.size() + 1L;
        List<String> urls = (request.receiptFileUrls() != null && !request.receiptFileUrls().isEmpty())
                ? request.receiptFileUrls()
                : (request.attachments() != null ? request.attachments() : List.of());
        MaintenanceExpenseRecordDto created = new MaintenanceExpenseRecordDto(
                nextId,
                "EXP-2026-00" + nextId,
                request.category() != null ? request.category() : "MAINTENANCE",
                request.title(),
                request.titleTe(),
                request.vendorName() != null ? request.vendorName() : "Sri Sai Services, Ongole",
                request.amount() != null ? request.amount() : 0.0,
                request.expenseDate() != null ? request.expenseDate() : "2026-04-05",
                request.paymentMode() != null ? request.paymentMode() : "NEFT",
                request.transactionReference() != null ? request.transactionReference() : "UTR-" + System.currentTimeMillis(),
                urls,
                urls,
                request.recordedByName() != null ? request.recordedByName() : "Treasurer (Elite Towers)",
                request.approvedByName() != null ? request.approvedByName() : "President (Elite Towers)",
                "APPROVED",
                request.notes(),
                Instant.now()
        );
        expenseRecords.add(0, created);
        return created;
    }

    private void seedDefaultEliteTowersExpenses() {
        String tenant = "aikya_assoc_1";
        writeAndBuildResponse("kone-lift-amc-invoice-apr2026.pdf", "ASSOCIATION_EXPENSE", "application/pdf",
                "Elite Towers Ongole - KONE Elevator AMC Receipt #INV-8812".getBytes(StandardCharsets.UTF_8), 48200, false);
        writeAndBuildResponse("kone-lift-neft-utr-proof.png", "ASSOCIATION_EXPENSE", "image/png",
                "SBI NEFT UTR SBIN426095881290 - Rs 18500".getBytes(StandardCharsets.UTF_8), 24100, false);
        writeAndBuildResponse("ongole-diesel-bunk-bill-102.jpg", "ASSOCIATION_EXPENSE", "image/jpeg",
                "Prakasam Fuels Ongole - 120L HSD Diesel for 125kVA DG Set".getBytes(StandardCharsets.UTF_8), 31900, false);
        writeAndBuildResponse("water-tanker-supply-voucher-apr.pdf", "ASSOCIATION_EXPENSE", "application/pdf",
                "Sri Venkateswara Water Tankers Ongole - 6 Loads Receipt".getBytes(StandardCharsets.UTF_8), 28400, false);

        String baseUrl = minioEndpoint + "/" + minioBucket + "/" + tenant;
        List<String> koneFiles = List.of(
                baseUrl + "/kone-lift-amc-invoice-apr2026.pdf",
                baseUrl + "/kone-lift-neft-utr-proof.png"
        );
        expenseRecords.add(new MaintenanceExpenseRecordDto(
                1L,
                "EXP-2026-001",
                "ELEVATOR_AMC",
                "KONE Lift Quarterly AMC (Block A & B)",
                "కోనే లిఫ్ట్ త్రైమాసిక నిర్వహణ (బ్లాక్ A & B)",
                "KONE Elevator India - Ongole Service Branch",
                18500.00,
                "2026-04-02",
                "NEFT",
                "SBIN426095881290",
                koneFiles,
                koneFiles,
                "Venkata Subba Rao (Treasurer)",
                "Narasimha Rao (President)",
                "APPROVED",
                "Quarterly preventive maintenance for 2 passenger lifts at Elite Towers, Ongole.",
                Instant.now().minusSeconds(86400 * 3)
        ));

        List<String> dieselFiles = List.of(baseUrl + "/ongole-diesel-bunk-bill-102.jpg");
        expenseRecords.add(new MaintenanceExpenseRecordDto(
                2L,
                "EXP-2026-002",
                "GENERATOR_DIESEL",
                "125 kVA DG Set Diesel Refill (120 Liters)",
                "125 kVA జనరేటర్ డీజిల్ రీఫిల్ (120 లీటర్లు)",
                "Prakasam Highway Fuels (HPCL), Ongole",
                11640.00,
                "2026-04-04",
                "UPI",
                "UPI-410299381721",
                dieselFiles,
                dieselFiles,
                "Venkata Subba Rao (Treasurer)",
                "Narasimha Rao (President)",
                "APPROVED",
                "Backup generator fuel stocked ahead of summer load shedding in Ongole.",
                Instant.now().minusSeconds(86400 * 2)
        ));

        List<String> waterFiles = List.of(baseUrl + "/water-tanker-supply-voucher-apr.pdf");
        expenseRecords.add(new MaintenanceExpenseRecordDto(
                3L,
                "EXP-2026-003",
                "WATER_TANKER",
                "Municipal Supplemental Water Tankers (6 Loads)",
                "మంచినీటి ట్యాంకర్ల సరఫరా (6 లోడ్లు)",
                "Sri Venkateswara Water Suppliers, Kurnool Road, Ongole",
                7200.00,
                "2026-04-05",
                "CHEQUE",
                "CHQ-004819 (Union Bank Ongole)",
                waterFiles,
                waterFiles,
                "Venkata Subba Rao (Treasurer)",
                "Narasimha Rao (President)",
                "APPROVED",
                "6 tanker loads (10,000L each) for sump replenishment.",
                Instant.now().minusSeconds(86400)
        ));
    }

    private StorageUploadResponse writeAndBuildResponse(
            String rawFileName,
            String category,
            String contentType,
            byte[] content,
            long sizeBytes,
            boolean pushToMinio
    ) {
        String tenant = TenantContext.getCurrentTenant();
        if (tenant == null || tenant.isBlank() || TenantContext.DEFAULT_TENANT.equalsIgnoreCase(tenant)) {
            tenant = "aikya_assoc_1";
        }
        String sanitizedTenant = sanitizeSegment(tenant);
        String sanitizedFileName = sanitizeFileName(rawFileName);
        String resolvedCategory = (category == null || category.isBlank()) ? "MAINTENANCE_PAYMENT" : category.trim().toUpperCase();
        String resolvedContentType = (contentType != null && !contentType.isBlank()) ? contentType : inferContentType(sanitizedFileName);
        byte[] safeBytes = content != null ? content : new byte[0];

        // 1. Always write local fallback copy in /tmp/aikya-uploads/<tenant>/<filename>
        Path tenantDir = rootStorageDir.resolve(sanitizedTenant);
        Path targetFile = tenantDir.resolve(sanitizedFileName);
        try {
            Files.createDirectories(tenantDir);
            Files.write(targetFile, safeBytes);
        } catch (IOException ex) {
            log.warn("Could not write file to local fallback {}: {}", targetFile, ex.getMessage());
        }

        // 2. Upload to MinIO S3 via AWS Signature V4 PUT
        String objectKey = sanitizedTenant + "/" + sanitizedFileName;
        String minioUrl = minioEndpoint + "/" + minioBucket + "/" + objectKey;
        if (pushToMinio) {
            uploadToMinioS3(objectKey, resolvedContentType, safeBytes);
        }

        Instant now = Instant.now();
        return new StorageUploadResponse(
                minioUrl,
                minioUrl,
                sanitizedFileName,
                resolvedCategory,
                resolvedCategory,
                resolvedContentType,
                "MINIO_S3",
                objectKey,
                targetFile.toAbsolutePath().toString(),
                sanitizedTenant,
                sizeBytes,
                sizeBytes,
                now,
                now
        );
    }

    private void uploadToMinioS3(String objectKey, String contentType, byte[] payload) {
        try {
            String canonicalUri = "/" + minioBucket + "/" + objectKey;
            URI uri = URI.create(minioEndpoint + canonicalUri);
            String hostHeader = uri.getPort() > 0 && uri.getPort() != 80 && uri.getPort() != 443
                    ? uri.getHost() + ":" + uri.getPort()
                    : uri.getHost();

            ZonedDateTime nowUtc = ZonedDateTime.now(ZoneOffset.UTC);
            String amzDate = AMZ_DATE_FORMAT.format(nowUtc);
            String dateStamp = DATE_STAMP_FORMAT.format(nowUtc);
            String payloadHash = sha256Hex(payload);

            String canonicalHeaders = "content-type:" + contentType + "\n"
                    + "host:" + hostHeader + "\n"
                    + "x-amz-content-sha256:" + payloadHash + "\n"
                    + "x-amz-date:" + amzDate + "\n";
            String signedHeaders = "content-type;host;x-amz-content-sha256;x-amz-date";
            String canonicalRequest = "PUT\n"
                    + canonicalUri + "\n"
                    + "\n"
                    + canonicalHeaders + "\n"
                    + signedHeaders + "\n"
                    + payloadHash;

            String credentialScope = dateStamp + "/" + minioRegion + "/s3/aws4_request";
            String stringToSign = "AWS4-HMAC-SHA256\n"
                    + amzDate + "\n"
                    + credentialScope + "\n"
                    + sha256Hex(canonicalRequest.getBytes(StandardCharsets.UTF_8));

            byte[] signingKey = getSignatureKey(minioSecretKey, dateStamp, minioRegion, "s3");
            String signature = HexFormat.of().formatHex(hmacSha256(signingKey, stringToSign));

            String authorizationHeader = "AWS4-HMAC-SHA256 "
                    + "Credential=" + minioAccessKey + "/" + credentialScope + ", "
                    + "SignedHeaders=" + signedHeaders + ", "
                    + "Signature=" + signature;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofMillis(2000))
                    .header("Content-Type", contentType)
                    .header("x-amz-content-sha256", payloadHash)
                    .header("x-amz-date", amzDate)
                    .header("Authorization", authorizationHeader)
                    .PUT(HttpRequest.BodyPublishers.ofByteArray(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Uploaded {} ({} bytes) to MinIO S3 at {}", objectKey, payload.length, uri);
            } else {
                log.debug("MinIO S3 PUT returned HTTP {} for {}: {}", response.statusCode(), uri, response.body());
            }
        } catch (Exception ex) {
            log.debug("MinIO S3 unreachable ({}), served from local fallback storage", ex.getMessage());
        }
    }

    private static String sha256Hex(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(data));
    }

    private static byte[] hmacSha256(byte[] key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] getSignatureKey(String key, String dateStamp, String regionName, String serviceName) throws Exception {
        byte[] kSecret = ("AWS4" + key).getBytes(StandardCharsets.UTF_8);
        byte[] kDate = hmacSha256(kSecret, dateStamp);
        byte[] kRegion = hmacSha256(kDate, regionName);
        byte[] kService = hmacSha256(kRegion, serviceName);
        return hmacSha256(kService, "aws4_request");
    }

    private String inferContentType(String filename) {
        if (filename == null) {
            return "application/octet-stream";
        }
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        } else if (lower.endsWith(".png")) {
            return "image/png";
        } else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        return "application/octet-stream";
    }

    private String sanitizeSegment(String segment) {
        if (segment == null || segment.isBlank()) {
            return "aikya_assoc_1";
        }
        String cleaned = segment.trim().replaceAll("[^a-zA-Z0-9_-]", "_");
        return cleaned.isBlank() ? "aikya_assoc_1" : cleaned;
    }

    private String sanitizeFileName(String input) {
        if (input == null || input.isBlank()) {
            return "file-" + System.currentTimeMillis() + ".dat";
        }
        String cleaned = input.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
        return cleaned.isBlank() ? "file-" + System.currentTimeMillis() + ".dat" : cleaned;
    }
}
