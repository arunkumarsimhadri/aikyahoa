package com.aikya.common;

import java.util.regex.Pattern;

public final class TenantContext {

    public static final String DEFAULT_TENANT = "public";
    public static final String TENANT_HEADER = "X-Tenant-ID";
    private static final Pattern VALID_SCHEMA_PATTERN = Pattern.compile("^(public|aikya_assoc_[a-zA-Z0-9_]+)$");

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setCurrentTenant(String tenantSchema) {
        if (tenantSchema == null || tenantSchema.isBlank()) {
            CURRENT_TENANT.set(DEFAULT_TENANT);
            return;
        }
        String normalized = tenantSchema.trim().toLowerCase();
        if (!VALID_SCHEMA_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid tenant schema identifier: " + tenantSchema);
        }
        CURRENT_TENANT.set(normalized);
    }

    public static String getCurrentTenant() {
        String tenant = CURRENT_TENANT.get();
        return (tenant == null || tenant.isBlank()) ? DEFAULT_TENANT : tenant;
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }

    public static boolean isValidSchemaName(String schemaName) {
        return schemaName != null && VALID_SCHEMA_PATTERN.matcher(schemaName.trim().toLowerCase()).matches();
    }
}

