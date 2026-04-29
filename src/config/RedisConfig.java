package com.nightout.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis configuration for caching.
 *
 * WHY CACHING?
 * The ranked venues query is expensive — it joins venues, nights, and rsvps,
 * then runs a scoring formula across all results. Running it on every request
 * would hammer the database. Redis keeps the result in memory so identical
 * requests return in <1ms instead of ~200ms.
 *
 * HOW IT WORKS WITH @Cacheable:
 * When Spring sees @Cacheable("rankedVenues") on a method, it:
 *   1. Builds a cache key from the method parameters
 *   2. Checks Redis for that key
 *   3a. CACHE HIT  → return the cached value, skip the method body entirely
 *   3b. CACHE MISS → run the method, store the result in Redis, return it
 *
 * @CacheEvict("rankedVenues") tells Spring to DELETE cache entries when
 * data changes (new venue, new rating, etc.) so stale data is never served.
 */
@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * RedisCacheManager defines cache behaviour per cache name.
     *
     * We configure different TTLs (Time To Live) for different caches:
     *   rankedVenues → 30 minutes  (rankings change slowly)
     *   nightDetails  → 5 minutes   (RSVPs change more frequently)
     *   userProfiles  → 10 minutes  (profiles rarely change)
     *
     * After TTL expires, Redis evicts the entry automatically.
     * The next request will be a cache miss → fresh DB query → re-cached.
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {

        // Default configuration — applies to any cache not explicitly configured below
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                // Serialise values as JSON (human-readable, debuggable in Redis CLI)
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                // Serialise keys as plain strings
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                // Don't cache null values — avoid polluting cache with missing data
                .disableCachingNullValues()
                // Default TTL: 10 minutes
                .entryTtl(Duration.ofMinutes(10));

        // Per-cache TTL overrides
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

        cacheConfigs.put("rankedVenues",
                defaultConfig.entryTtl(Duration.ofMinutes(30)));

        cacheConfigs.put("nightDetails",
                defaultConfig.entryTtl(Duration.ofMinutes(5)));

        cacheConfigs.put("userProfiles",
                defaultConfig.entryTtl(Duration.ofMinutes(10)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }

    /**
     * RedisTemplate is used for manual Redis operations (outside @Cacheable).
     * For example: storing session data, rate limiting counters, etc.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}