package com.vcc.template.common.cache.config;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Configuration properties for Redis cache settings.
 * Allows environment-specific cache configuration with production optimizations.
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.cache")
@Validated
public class CacheProperties {

    /**
     * Whether caching is enabled globally.
     */
    private boolean enabled = true;

    /**
     * Default TTL for cache entries.
     */
    @NotNull
    private Duration defaultTtl = Duration.ofHours(1);

    /**
     * Project-specific cache settings.
     */
    private ProjectCache project = new ProjectCache();

    @Data
    public static class ProjectCache {
        /**
         * TTL for individual project data.
         */
        @NotNull
        private Duration projectTtl = Duration.ofHours(1);

        /**
         * TTL for project list results.
         */
        @NotNull
        private Duration listTtl = Duration.ofMinutes(15);

        /**
         * TTL for project existence checks.
         */
        @NotNull
        private Duration existsTtl = Duration.ofMinutes(30);
    }
}