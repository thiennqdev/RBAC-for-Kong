package com.vcc.template.common.tenant;

import com.vcc.template.common.security.KongTrustedAuthenticationToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TenantContextFilter extends OncePerRequestFilter {

    @Value("${security.jwt.tenant-claim-name:tenant_id}")
    private String tenantClaimName;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String tenant = null;

            // 1. Try to extract from URL path (e.g., /projects/{projectCode}/roles, /providers, /members, etc.)
            String path = request.getRequestURI();
            if (path.contains("/projects/")) {
                String[] segments = path.split("/");
                for (int i = 0; i < segments.length; i++) {
                    if ("projects".equals(segments[i]) && i + 1 < segments.length) {
                        String candidate = segments[i + 1];
                        // Only treat as tenant if there is a sub-resource after projectCode
                        // e.g., /projects/{code}/roles — NOT /projects (list endpoint)
                        if (!candidate.isEmpty() && i + 2 < segments.length) {
                            tenant = candidate;
                            break;
                        }
                    }
                }
            }

            // 2. Fallback to JWT claim (using Kong-trusted authentication)
            if (!StringUtils.hasText(tenant)) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication instanceof KongTrustedAuthenticationToken kongAuth) {
                    tenant = kongAuth.getClaimAsString(tenantClaimName);
                }
            }

            if (StringUtils.hasText(tenant)) {
                TenantContext.setCurrentTenant(tenant);
            } else {
                TenantContext.setCurrentTenant(TenantSchemaResolver.DEFAULT_TENANT);
            }

            filterChain.doFilter(request, response);

        } finally {
            TenantContext.clear();
        }
    }
}

