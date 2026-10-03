package com.vcc.template.common.cache.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis cache configuration with tenant-aware key generation, production
 * optimizations,
 * and environment-specific settings for performance tuning.
 *
 * IMPORTANT: All DTOs used in @Cacheable methods MUST be regular classes (not
 * records),
 * because Jackson's DefaultTyping.NON_FINAL only adds type information for
 * non-final types.
 * Java records are final and will cause deserialization errors if cached.
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "app.cache.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class RedisCacheConfig {

    private final CacheProperties cacheProperties;
    private final Environment environment;

    @Bean(name = "redisObjectMapper")
    public ObjectMapper redisObjectMapper() {
        return JsonMapper.builder()
                .activateDefaultTyping(BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType("com.vcc.template")
                        .allowIfSubType("java.util")
                        .build(), DefaultTyping.NON_FINAL)
                .build();
    }

    @Bean("cacheKeyObjectMapper")
    public ObjectMapper cacheKeyObjectMapper() {
        return JsonMapper.builder()
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
                .build();
    }

    // Cache region names
    public static final String PROJECTS_CACHE = "projects";
    public static final String PROJECT_LISTS_CACHE = "project-lists";
    public static final String PROJECT_EXISTS_CACHE = "project-exists";

    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = createDefaultCacheConfiguration();
        Map<String, RedisCacheConfiguration> cacheConfigurations = createCacheConfigurations();

        RedisCacheManager.RedisCacheManagerBuilder builder = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware();

        RedisCacheManager cacheManager = builder.build();

        log.info("Redis cache manager configured with {} cache regions for {} environment",
                cacheConfigurations.size(), getCurrentEnvironment());
        return cacheManager;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Configure serializers
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        RedisSerializer<Object> jsonSerializer = createGenericJacksonJsonRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.setDefaultSerializer(jsonSerializer);
        template.setEnableTransactionSupport(true);
        template.afterPropertiesSet();

        log.info("Redis template configured with Generic Jackson JSON serialization and transaction support");
        return template;
    }

    /**
     * Creates a default cache configuration with Generic Jackson JSON serialization
     * and environment-specific TTL.
     * Uses GenericJacksonJsonRedisSerializer to preserve type information during
     * serialization/deserialization.
     */
    private RedisCacheConfiguration createDefaultCacheConfiguration() {
        Duration ttl = cacheProperties.getDefaultTtl();

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(createGenericJacksonJsonRedisSerializer()));
    }

    /**
     * Creates cache-specific configurations with environment-optimized TTL
     * policies.
     */
    private Map<String, RedisCacheConfiguration> createCacheConfigurations() {
        Map<String, RedisCacheConfiguration> configurations = new HashMap<>();
        RedisCacheConfiguration baseConfig = createDefaultCacheConfiguration();

        CacheProperties.ProjectCache projectCache = cacheProperties.getProject();

        // Individual project cache - environment-specific TTL
        configurations.put(PROJECTS_CACHE, baseConfig.entryTtl(projectCache.getProjectTtl()));

        // Project lists cache - shorter TTL for volatile data
        configurations.put(PROJECT_LISTS_CACHE, baseConfig.entryTtl(projectCache.getListTtl()));

        // Project existence checks - medium TTL
        configurations.put(PROJECT_EXISTS_CACHE, baseConfig.entryTtl(projectCache.getExistsTtl()));

        log.debug("Cache configurations created for {} environment: projects={}min, lists={}min, exists={}min",
                getCurrentEnvironment(),
                projectCache.getProjectTtl().toMinutes(),
                projectCache.getListTtl().toMinutes(),
                projectCache.getExistsTtl().toMinutes());

        return configurations;
    }

    private String getCurrentEnvironment() {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length > 0) {
            return String.join(",", activeProfiles);
        }
        return "default";
    }

    /**
     * Creates a GenericJacksonJsonRedisSerializer with proper type information
     * handling.
     */
    private RedisSerializer<Object> createGenericJacksonJsonRedisSerializer() {
        return new GenericJacksonJsonRedisSerializer(redisObjectMapper());
    }
}
