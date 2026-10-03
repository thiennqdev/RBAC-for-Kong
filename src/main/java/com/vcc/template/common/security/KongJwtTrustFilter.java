package com.vcc.template.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.*;

/**
 * A filter that trusts JWT tokens already verified by Kong Gateway.
 *
 * <p>This filter extracts the JWT token from the Authorization header,
 * decodes the payload (Base64, no signature verification), and populates
 * the Spring SecurityContext with the user's identity and roles.</p>
 *
 * <p><b>IMPORTANT:</b> This filter does NOT verify the JWT signature.
 * It trusts that Kong Gateway has already validated the token before
 * forwarding the request to this backend service. Therefore, this
 * backend MUST NOT be directly accessible from outside — it should
 * only receive traffic through Kong.</p>
 */
@Component
@Slf4j
public class KongJwtTrustFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String ROLES_KEY = "roles";

    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Value("${gateway.internal.secret:}")
    private String gatewayInternalSecret;

    public KongJwtTrustFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // Perimeter security check: if a shared internal key is configured, verify it
        if (org.springframework.util.StringUtils.hasText(gatewayInternalSecret)) {
            String incomingSecret = request.getHeader("X-Gateway-Internal-Key");
            if (!gatewayInternalSecret.equals(incomingSecret)) {
                log.warn("Direct access rejected: missing or invalid X-Gateway-Internal-Key from {}", request.getRemoteAddr());
                filterChain.doFilter(request, response);
                return;
            }
        }

        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length());
            try {
                Map<String, Object> claims = decodeJwtPayload(token);
                String subject = (String) claims.get("sub");
                String preferredUsername = (String) claims.getOrDefault("preferred_username", subject);

                // Extract roles from realm_access.roles (Keycloak standard structure)
                Collection<GrantedAuthority> authorities = extractAuthorities(claims);

                // Build a KongTrustedAuthenticationToken that carries all JWT claims
                KongTrustedAuthenticationToken authentication =
                        new KongTrustedAuthenticationToken(preferredUsername, token, authorities, claims);

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Kong-trusted authentication set for user: {} with roles: {}",
                        preferredUsername, authorities);

            } catch (Exception e) {
                log.warn("Failed to decode JWT payload from Kong-forwarded request: {}", e.getMessage());
                // Don't set authentication — let Spring Security handle as unauthenticated
            }
        } else {
            log.debug("No valid Authorization header found in Kong-forwarded request.");
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Decodes the JWT payload section (Base64URL) without verifying the signature.
     * Kong has already verified the token, so we only need to read the claims.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> decodeJwtPayload(String token) {
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid JWT format: expected at least 2 parts");
        }

        try {
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            return objectMapper.readValue(payloadBytes, new tools.jackson.core.type.TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("Failed to parse JWT payload JSON: {}", e.getMessage());
            throw new RuntimeException("Invalid JWT payload", e);
        }
    }

    /**
     * Extracts GrantedAuthority list from the realm_access.roles claim.
     * This mirrors the behavior of the existing GrantedAuthoritiesExtractor
     * but works with raw Map claims instead of Spring's Jwt object.
     */
    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractAuthorities(Map<String, Object> claims) {
        Object realmAccess = claims.get(REALM_ACCESS_CLAIM);
        if (realmAccess instanceof Map<?, ?> realmAccessMap) {
            Object roles = realmAccessMap.get(ROLES_KEY);
            if (roles instanceof List<?> roleList) {
                List<GrantedAuthority> authorities = new ArrayList<>();
                for (Object role : roleList) {
                    authorities.add(new SimpleGrantedAuthority(role.toString()));
                }
                log.debug("Extracted roles from JWT realm_access: {}", authorities);
                return Collections.unmodifiableList(authorities);
            }
        }
        log.warn("No realm_access.roles found in JWT. Available claims: {}", claims.keySet());
        return Collections.emptyList();
    }
}
