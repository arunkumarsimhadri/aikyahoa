package com.aikya.platform.security;

import com.aikya.common.AuthenticatedUser;
import com.aikya.common.Role;
import com.aikya.common.TenantContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long jwtExpirationMs;

    public JwtService(
            @Value("${aikya.security.jwt-secret:aW5kaWFuLWFwYXJ0bWVudC1haWt5YS1zZWNyZXQta2V5LTIwMjYtdW5pdHktb25lbmVzcw==}") String jwtSecret,
            @Value("${aikya.security.jwt-expiration-ms:86400000}") long jwtExpirationMs
    ) {
        this.signingKey = buildSecretKey(jwtSecret);
        this.jwtExpirationMs = jwtExpirationMs;
    }

    private SecretKey buildSecretKey(String secret) {
        byte[] keyBytes;
        try {
            byte[] decoded = Decoders.BASE64.decode(secret);
            keyBytes = decoded.length >= 32 ? decoded : sha256(secret);
        } catch (Exception ex) {
            byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
            keyBytes = raw.length >= 32 ? raw : sha256(secret);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private byte[] sha256(String input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    public String generateToken(AuthenticatedUser user) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(jwtExpirationMs);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.userId());
        claims.put("phoneNumber", user.phoneNumber());
        claims.put("fullName", user.fullName());
        claims.put("preferredLocale", user.preferredLocale() != null ? user.preferredLocale() : "en");
        claims.put("associationId", user.associationId());
        claims.put("tenantSchema", user.tenantSchema() != null ? user.tenantSchema() : TenantContext.DEFAULT_TENANT);
        claims.put("role", user.role() != null ? user.role().name() : Role.OWNER.name());
        claims.put("unitNumber", user.unitNumber());
        claims.put("blockName", user.blockName());

        return Jwts.builder()
                .subject(user.userId() != null ? String.valueOf(user.userId()) : user.phoneNumber())
                .claims(claims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public AuthenticatedUser parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Long userId = toLong(claims.get("userId"));
        String phoneNumber = claims.get("phoneNumber", String.class);
        String fullName = claims.get("fullName", String.class);
        String preferredLocale = claims.get("preferredLocale", String.class);
        Long associationId = toLong(claims.get("associationId"));
        String tenantSchema = claims.get("tenantSchema", String.class);
        String roleStr = claims.get("role", String.class);
        Role role = Role.OWNER;
        if (roleStr != null && !roleStr.isBlank()) {
            try {
                role = Role.valueOf(roleStr);
            } catch (IllegalArgumentException ignored) {
                role = Role.OWNER;
            }
        }
        String unitNumber = claims.get("unitNumber", String.class);
        String blockName = claims.get("blockName", String.class);

        return new AuthenticatedUser(
                userId,
                phoneNumber,
                fullName,
                preferredLocale != null ? preferredLocale : "en",
                associationId,
                tenantSchema != null ? tenantSchema : TenantContext.DEFAULT_TENANT,
                role,
                unitNumber,
                blockName
        );
    }

    public boolean isTokenValid(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public long getExpirationSeconds() {
        return jwtExpirationMs / 1000L;
    }

    private Long toLong(Object val) {
        if (val instanceof Number number) {
            return number.longValue();
        }
        if (val instanceof String str && !str.isBlank()) {
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
