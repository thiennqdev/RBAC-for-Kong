package com.vcc.template.common.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Map;

/**
 * Authentication token representing a user whose JWT has been verified by Kong Gateway.
 *
 * <p>This token carries the full set of decoded JWT claims so that downstream
 * filters and services can extract user information (subject, tenant, roles)
 * without needing to re-parse the raw token string.</p>
 *
 * <p>Compatible with code that previously checked for {@code JwtAuthenticationToken}
 * by providing equivalent accessor methods: {@link #getSubject()}, {@link #getClaimAsString(String)}.</p>
 */
public class KongTrustedAuthenticationToken extends UsernamePasswordAuthenticationToken {

    private final Map<String, Object> claims;
    private final String rawToken;

    public KongTrustedAuthenticationToken(Object principal, String rawToken,
                                          Collection<? extends GrantedAuthority> authorities,
                                          Map<String, Object> claims) {
        super(principal, rawToken, authorities);
        this.rawToken = rawToken;
        this.claims = claims;
    }

    /**
     * Returns all decoded JWT claims.
     */
    public Map<String, Object> getClaims() {
        return claims;
    }

    /**
     * Returns the JWT "sub" claim (user's Keycloak ID).
     */
    public String getSubject() {
        Object sub = claims.get("sub");
        return sub != null ? sub.toString() : null;
    }

    /**
     * Returns a specific claim value as a String.
     *
     * @param claimName the name of the claim
     * @return the claim value as a string, or null if not present
     */
    public String getClaimAsString(String claimName) {
        Object value = claims.get(claimName);
        return value != null ? value.toString() : null;
    }

    /**
     * Returns the raw JWT token string.
     */
    public String getRawToken() {
        return rawToken;
    }
}
