package com.vcc.template.common.cache.config;

import com.vcc.template.common.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Tenant-aware cache key generator that prefixes cache keys with tenant
 * context.
 * Ensures proper tenant isolation in multi-tenant caching scenarios.
 * Generates deterministic cache keys based on object content rather than hash
 * codes.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TenantAwareCacheKeyGenerator implements KeyGenerator {

    private static final String DEFAULT_TENANT = "default";
    private static final String KEY_SEPARATOR = ":";
    private static final int HASH_LEN = 16; // 64-bit

    @Qualifier("cacheKeyObjectMapper")
    private final ObjectMapper cacheKeyObjectMapper;

    @Override
    public Object generate(@NonNull Object target, @NonNull Method method, Object @NonNull... params) {
        String tenantId = resolveTenantId();
        String base = tenantId + KEY_SEPARATOR
                + target.getClass().getSimpleName() + KEY_SEPARATOR
                + method.getName();

        StringBuilder cacheKey = new StringBuilder(base);
        if (params.length > 0) {
            cacheKey.append(KEY_SEPARATOR).append(hashParams(params));
        }
        if (log.isDebugEnabled())
            log.debug("Generated cache key: {} for tenant: {}, method: {}", cacheKey, tenantId, method.getName());
        return cacheKey.toString();
    }

    /**
     * Resolves the current tenant ID with fallback to default tenant.
     * Normalizes tenant ID to lowercase for consistent key generation.
     */
    private String resolveTenantId() {
        String tenant = TenantContext.getCurrentTenant();
        if (tenant == null || tenant.trim().isEmpty() || "public".equals(tenant)) {
            return DEFAULT_TENANT;
        }
        return tenant.toLowerCase().trim();
    }

    private String hashParams(Object[] params) {
        String json = cacheKeyObjectMapper.writeValueAsString(params);
        if (log.isDebugEnabled())
            log.debug("Params JSON: {}", json);
        return hash(json);
    }

    private String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(encoded).substring(0, HASH_LEN); // rút gọn
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}