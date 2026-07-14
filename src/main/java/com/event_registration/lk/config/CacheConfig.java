package com.event_registration.lk.config;

import java.time.Duration;

import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.LoggingCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis-backed cache layer.
 *
 * Values are stored as JSON (with embedded type info) so entries are readable
 * in redis-cli and cached DTOs don't need to implement java.io.Serializable.
 *
 * Per-cache TTLs:
 *  - events (60s): the cached list embeds availableSeats, which every booking
 *    mutates. A short TTL bounds that staleness without evicting on each
 *    booking; booking correctness is unaffected because seat checks run under
 *    a pessimistic row lock, never against the cache.
 */
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    public static final String EVENTS_CACHE = "events";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .disableCachingNullValues()
                .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .withCacheConfiguration(EVENTS_CACHE, defaults.entryTtl(Duration.ofSeconds(60)))
                .build();
    }

    /**
     * A Redis outage degrades to a cache miss (logged at WARN) instead of
     * failing the request — the DB remains the source of truth.
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler();
    }
}
