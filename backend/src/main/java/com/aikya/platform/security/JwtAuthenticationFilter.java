package com.aikya.platform.security;

import com.aikya.common.AuthenticatedUser;
import com.aikya.common.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            AuthenticatedUser authenticatedUser = null;
            String authHeader = request.getHeader(AUTHORIZATION_HEADER);

            if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
                String token = authHeader.substring(BEARER_PREFIX.length()).trim();
                if (!token.isEmpty() && jwtService.isTokenValid(token)) {
                    try {
                        authenticatedUser = jwtService.parseToken(token);
                        List<SimpleGrantedAuthority> authorities = authenticatedUser.role() != null
                                ? List.of(new SimpleGrantedAuthority("ROLE_" + authenticatedUser.role().name()))
                                : Collections.emptyList();

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(authenticatedUser, null, authorities);
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    } catch (Exception ex) {
                        log.debug("Failed to authenticate JWT token: {}", ex.getMessage());
                    }
                }
            }

            String headerTenant = request.getHeader(TenantContext.TENANT_HEADER);
            String resolvedTenant = TenantContext.DEFAULT_TENANT;

            if (headerTenant != null && !headerTenant.isBlank() && TenantContext.isValidSchemaName(headerTenant)) {
                resolvedTenant = headerTenant.trim().toLowerCase();
            } else if (authenticatedUser != null
                    && authenticatedUser.tenantSchema() != null
                    && TenantContext.isValidSchemaName(authenticatedUser.tenantSchema())) {
                resolvedTenant = authenticatedUser.tenantSchema().trim().toLowerCase();
            }

            TenantContext.setCurrentTenant(resolvedTenant);
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
