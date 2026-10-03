package com.vcc.template.common.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.*;
import java.util.stream.Collectors;

public class GrantedAuthoritiesExtractor implements Converter<Jwt, Collection<GrantedAuthority>> {
    private static final String RESOURCE_ACCESS = "realm_access";
    private static final String KEYCLOAK_ROLE_KEY = "roles";
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GrantedAuthoritiesExtractor.class);

    @Override
    @SuppressWarnings("unchecked")
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Map<String, Object> resourceAccess = jwt.getClaimAsMap(RESOURCE_ACCESS);
        if (Objects.nonNull(resourceAccess) && resourceAccess.containsKey(KEYCLOAK_ROLE_KEY)) {
            List<String> roleList = (List<String>) resourceAccess.get(KEYCLOAK_ROLE_KEY);
            log.debug("Extracted roles from JWT realm_access: {}", roleList);
            return roleList.stream().map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toUnmodifiableList());
        }
        log.warn("No realm_access.roles found in JWT. Available claims: {}", jwt.getClaims().keySet());
        return Collections.emptyList();
    }
}
