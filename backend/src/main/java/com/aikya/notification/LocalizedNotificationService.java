package com.aikya.notification;

import com.aikya.common.NotificationPort;
import com.aikya.common.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class LocalizedNotificationService implements NotificationPort {

    private static final Logger log = LoggerFactory.getLogger(LocalizedNotificationService.class);

    private static final Map<String, Map<String, String>> TEMPLATES = Map.of(
            "OTP_LOGIN", Map.of(
                    "en", "[Aikya RWA] Your login OTP is [[${otpCode}]]. Valid for 5 minutes. Do not share with anyone.",
                    "te", "[ఐక్య RWA] మీ లాగిన్ OTP: [[${otpCode}]]. ఇది 5 నిమిషాలు మాత్రమే చెల్లుతుంది. ఎవరితోనూ పంచుకోవద్దు."
            ),
            "INVOICE_GENERATED", Map.of(
                    "en", "[Aikya RWA] Maintenance Invoice [[${invoiceNumber}]] for Flat [[${unitNumber}]] of Rs.[[${amount}]] is generated. Due date: [[${dueDate}]].",
                    "te", "[ఐక్య RWA] ఫ్లాట్ [[${unitNumber}]] కొరకు మెయింటెనెన్స్ బిల్లు [[${invoiceNumber}]] రూ.[[${amount}]] జారీ చేయబడింది. చివరి తేదీ: [[${dueDate}]]."
            ),
            "PAYMENT_RECONCILED", Map.of(
                    "en", "[Aikya RWA] Your payment of Rs.[[${amount}]] (Ref: [[${referenceNumber}]]) for Flat [[${unitNumber}]] has been [[${decision}]] by Treasurer. Notes: [[${auditNotes}]]",
                    "te", "[ఐక్య RWA] ఫ్లాట్ [[${unitNumber}]] కొరకు మీరు చెల్లించిన రూ.[[${amount}]] (Ref: [[${referenceNumber}]]) కోశాధికారిచే [[${decision}]] చేయబడింది. వివరాలు: [[${auditNotes}]]"
            ),
            "TICKET_STATUS_UPDATED", Map.of(
                    "en", "[Aikya RWA] Helpdesk Ticket [[${ticketNumber}]] ([[${category}]]) status updated to [[${status}]]. Technician: [[${technician}]]",
                    "te", "[ఐక్య RWA] మీ ఫిర్యాదు టికెట్ [[${ticketNumber}]] ([[${category}]]) స్థితి [[${status}]] కు మార్చబడింది. టెక్నీషియన్: [[${technician}]]"
            )
    );

    private final NotificationLogRepository notificationLogRepository;
    private final SpringTemplateEngine textTemplateEngine;
    private final CopyOnWriteArrayList<NotificationLogDto> inMemoryLogs = new CopyOnWriteArrayList<>();
    private final AtomicLong memoryIdGenerator = new AtomicLong(10000L);

    public LocalizedNotificationService(NotificationLogRepository notificationLogRepository) {
        this.notificationLogRepository = notificationLogRepository;
        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode(TemplateMode.TEXT);
        resolver.setCacheable(false);
        this.textTemplateEngine = new SpringTemplateEngine();
        this.textTemplateEngine.setTemplateResolver(resolver);
    }

    @Override
    public void sendNotification(Long recipientUserId,
                                 String phoneNumber,
                                 String recipientName,
                                 String preferredLocale,
                                 String channel,
                                 String templateKey,
                                 Map<String, Object> variables) {
        dispatchAndLog(recipientUserId, phoneNumber, recipientName, preferredLocale, channel, templateKey, variables);
    }

    public NotificationLogDto dispatchAndLog(Long recipientUserId,
                                             String phoneNumber,
                                             String recipientName,
                                             String preferredLocale,
                                             String channel,
                                             String templateKey,
                                             Map<String, Object> variables) {
        String normalizedLocale = normalizeLocale(preferredLocale);
        String resolvedChannel = (channel == null || channel.isBlank()) ? "SMS" : channel.trim().toUpperCase();
        String resolvedKey = (templateKey == null || templateKey.isBlank()) ? "INVOICE_GENERATED" : templateKey.trim().toUpperCase();
        String safePhone = (phoneNumber == null || phoneNumber.isBlank()) ? "+919848012345" : phoneNumber.trim();

        String rendered = renderTemplate(resolvedKey, normalizedLocale, variables);
        Instant now = Instant.now();

        Long logId = memoryIdGenerator.incrementAndGet();
        String currentTenant = TenantContext.getCurrentTenant();
        if (currentTenant != null && !TenantContext.DEFAULT_TENANT.equalsIgnoreCase(currentTenant)) {
            try {
                NotificationLog entity = new NotificationLog();
                entity.setRecipientUserId(recipientUserId);
                entity.setRecipientPhone(safePhone);
                entity.setRecipientName(recipientName);
                entity.setChannel(resolvedChannel);
                entity.setTemplateKey(resolvedKey);
                entity.setLocale(normalizedLocale);
                entity.setRenderedMessage(rendered);
                entity.setStatus("DELIVERED");
                entity.setCreatedAt(now);
                NotificationLog saved = notificationLogRepository.save(entity);
                if (saved.getId() != null) {
                    logId = saved.getId();
                }
            } catch (Exception ex) {
                log.debug("Notification persisted to in-memory buffer only (tenant={}): {}", currentTenant, ex.getMessage());
            }
        }

        NotificationLogDto dto = new NotificationLogDto(
                logId,
                recipientUserId,
                safePhone,
                recipientName,
                resolvedChannel,
                resolvedKey,
                normalizedLocale,
                rendered,
                "DELIVERED",
                now
        );
        inMemoryLogs.add(0, dto);
        while (inMemoryLogs.size() > 100) {
            inMemoryLogs.remove(inMemoryLogs.size() - 1);
        }

        log.info("[AIKYA-NOTIFY][{}][{}][{}] To {} ({}): {}",
                resolvedChannel, normalizedLocale, resolvedKey, safePhone, recipientName, rendered);
        return dto;
    }

    public String renderTemplate(String templateKey, String locale, Map<String, Object> variables) {
        String normalizedLocale = normalizeLocale(locale);
        String resolvedKey = (templateKey == null || templateKey.isBlank()) ? "INVOICE_GENERATED" : templateKey.trim().toUpperCase();

        Map<String, Object> mergedVariables = new HashMap<>(defaultSampleVariables(resolvedKey, normalizedLocale));
        if (variables != null) {
            variables.forEach((k, v) -> {
                if (v != null) {
                    mergedVariables.put(k, v);
                }
            });
        }

        String templateContent = loadTemplateContent(resolvedKey, normalizedLocale);
        Context context = new Context("te".equals(normalizedLocale) ? new Locale("te", "IN") : Locale.ENGLISH);
        context.setVariables(mergedVariables);
        return textTemplateEngine.process(templateContent, context).trim();
    }

    public List<NotificationLogDto> getRecentLogs() {
        List<NotificationLogDto> combined = new ArrayList<>();
        Set<String> seenSignatures = new HashSet<>();

        String currentTenant = TenantContext.getCurrentTenant();
        if (currentTenant != null && !TenantContext.DEFAULT_TENANT.equalsIgnoreCase(currentTenant)) {
            try {
                List<NotificationLog> dbLogs = notificationLogRepository.findTop50ByOrderByCreatedAtDesc();
                for (NotificationLog entity : dbLogs) {
                    NotificationLogDto dto = NotificationLogDto.fromEntity(entity);
                    combined.add(dto);
                    seenSignatures.add(signatureOf(dto));
                }
            } catch (Exception ex) {
                log.debug("Could not query notification_logs in tenant {}: {}", currentTenant, ex.getMessage());
            }
        }

        for (NotificationLogDto memDto : inMemoryLogs) {
            if (seenSignatures.add(signatureOf(memDto))) {
                combined.add(memDto);
            }
        }

        combined.sort(Comparator.comparing(NotificationLogDto::createdAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return combined.size() > 50 ? combined.subList(0, 50) : combined;
    }

    private String signatureOf(NotificationLogDto dto) {
        return dto.recipientPhone() + "|" + dto.templateKey() + "|" + dto.locale() + "|" + dto.renderedMessage();
    }

    private String loadTemplateContent(String templateKey, String locale) {
        String path = "templates/notifications/" + templateKey + "_" + locale + ".txt";
        try {
            ClassPathResource resource = new ClassPathResource(path);
            if (resource.exists()) {
                try (InputStream is = resource.getInputStream()) {
                    String content = new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
                    if (!content.isEmpty()) {
                        return content;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        Map<String, String> byLocale = TEMPLATES.getOrDefault(templateKey, TEMPLATES.get("INVOICE_GENERATED"));
        return byLocale.getOrDefault(locale, byLocale.get("en"));
    }

    private String normalizeLocale(String locale) {
        if (locale != null && locale.trim().toLowerCase().startsWith("te")) {
            return "te";
        }
        return "en";
    }

    private Map<String, Object> defaultSampleVariables(String templateKey, String locale) {
        boolean isTe = "te".equals(locale);
        return switch (templateKey) {
            case "OTP_LOGIN" -> Map.of("otpCode", "482910");
            case "INVOICE_GENERATED" -> Map.of(
                    "invoiceNumber", "INV-2026-04-A101",
                    "unitNumber", "A-101",
                    "amount", "7250.00",
                    "dueDate", "2026-04-10"
            );
            case "PAYMENT_RECONCILED" -> Map.of(
                    "amount", "7250.00",
                    "referenceNumber", "HDFC-NEFT-882910",
                    "unitNumber", "A-101",
                    "decision", isTe ? "ఆమోదించబడింది (VERIFIED)" : "VERIFIED",
                    "auditNotes", isTe ? "బ్యాంక్ స్టేట్‌మెంట్‌తో సరిపోలింది" : "Verified against SBI RWA account statement"
            );
            case "TICKET_STATUS_UPDATED" -> Map.of(
                    "ticketNumber", "TKT-1004",
                    "category", isTe ? "ప్లంబింగ్ (PLUMBING)" : "PLUMBING",
                    "status", isTe ? "పరిష్కరించబడింది (RESOLVED)" : "RESOLVED",
                    "technician", "Ramesh Kumar (+91 98480 55112)"
            );
            default -> Map.of();
        };
    }
}
