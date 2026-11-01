package com.aikya.platform.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class MockSmsGatewayProvider implements SmsGatewayProvider {

    private static final Logger log = LoggerFactory.getLogger(MockSmsGatewayProvider.class);
    private static final int MAX_HISTORY = 200;

    public record SmsDispatchRecord(
            String phoneNumber,
            String message,
            String locale,
            Instant sentAt
    ) {
    }

    private final CopyOnWriteArrayList<SmsDispatchRecord> recentDispatches = new CopyOnWriteArrayList<>();

    @Override
    public void sendOtpSms(String phoneNumber, String otpCode, String locale) {
        String resolvedLocale = (locale != null && locale.toLowerCase().startsWith("te")) ? "te" : "en";
        String message;
        if ("te".equals(resolvedLocale)) {
            message = String.format(
                    "[ఐక్య - Aikya] మీ లాగిన్ OTP: %s. ఇది 5 నిమిషాల వరకు చెల్లుబాటు అవుతుంది. ఎవరితోనూ పంచుకోవద్దు.",
                    otpCode
            );
        } else {
            message = String.format(
                    "[Aikya - ఐక్య] Your login OTP is %s. Valid for 5 minutes. Do not share this code with anyone.",
                    otpCode
            );
        }
        recordAndLog(phoneNumber, message, resolvedLocale);
    }

    @Override
    public void sendSms(String phoneNumber, String message) {
        recordAndLog(phoneNumber, message, "en");
    }

    private void recordAndLog(String phoneNumber, String message, String locale) {
        if (recentDispatches.size() >= MAX_HISTORY) {
            recentDispatches.remove(0);
        }
        recentDispatches.add(new SmsDispatchRecord(phoneNumber, message, locale, Instant.now()));
        log.info("[MOCK-SMS-GATEWAY] To={} | Locale={} | Message={}", phoneNumber, locale, message);
    }

    public List<SmsDispatchRecord> getRecentDispatches() {
        return Collections.unmodifiableList(recentDispatches);
    }
}
