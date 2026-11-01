package com.aikya.platform.dto;

public record UpdateLocaleRequest(
        String preferredLocale,
        String locale
) {
    public UpdateLocaleRequest(String preferredLocale) {
        this(preferredLocale, preferredLocale);
    }

    public String resolvedLocale() {
        if (preferredLocale != null && !preferredLocale.isBlank()) {
            return preferredLocale.trim().toLowerCase();
        }
        if (locale != null && !locale.isBlank()) {
            return locale.trim().toLowerCase();
        }
        return "en";
    }
}
