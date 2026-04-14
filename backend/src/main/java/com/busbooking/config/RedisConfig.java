package com.busbooking.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

@Configuration
@EnableCaching
@Slf4j
public class RedisConfig {

    @Value("${spring.data.redis.url:}")
    private String redisUrl;

    @Value("${app.cache.prefix-version:v5::}")
    private String cachePrefixVersion;

    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        try {
            log.info("Configured Redis endpoint: {}", maskRedisUrl(redisUrl));
            connectionFactory.getConnection().ping();

            GenericJackson2JsonRedisSerializer valueSerializer =
                    new GenericJackson2JsonRedisSerializer(buildCacheObjectMapper());

            RedisCacheConfiguration baseConfig = RedisCacheConfiguration.defaultCacheConfig()
                    .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                    .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer))
                    .computePrefixWith(cacheName -> cachePrefixVersion + cacheName + "::")
                    .disableCachingNullValues();

            Map<String, RedisCacheConfiguration> cacheConfigs = Map.of(
                    "busSearch", baseConfig.entryTtl(Duration.ofMinutes(5)),
                    "allBuses", baseConfig.entryTtl(Duration.ofMinutes(10)),
                    "seatAvailability", baseConfig.entryTtl(Duration.ofSeconds(30))
            );

            log.info("Redis is available - caching enabled with regions: busSearch, allBuses, seatAvailability");
            return RedisCacheManager.builder(connectionFactory)
                    .cacheDefaults(baseConfig.entryTtl(Duration.ofMinutes(5)))
                    .withInitialCacheConfigurations(cacheConfigs)
                    .transactionAware()
                    .build();
        } catch (Exception ex) {
            log.warn("Redis is unavailable - caching disabled. Reason: {}", ex.getMessage());
            return new NoOpCacheManager();
        }
    }

    @Bean
    public CacheErrorHandler cacheErrorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache GET failed for cache='{}', key='{}'. Evicting bad entry and falling back to DB.",
                        cache == null ? "unknown" : cache.getName(), key, exception);
                safelyEvict(cache, key);
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("Cache PUT failed for cache='{}', key='{}'. Request will continue without cache write.",
                        cache == null ? "unknown" : cache.getName(), key, exception);
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache EVICT failed for cache='{}', key='{}'.", cache == null ? "unknown" : cache.getName(), key, exception);
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Cache CLEAR failed for cache='{}'.", cache == null ? "unknown" : cache.getName(), exception);
            }

            private void safelyEvict(Cache cache, Object key) {
                if (cache == null || key == null) {
                    return;
                }
                try {
                    cache.evict(key);
                } catch (Exception ignored) {
                    log.debug("Failed to evict problematic key after cache read error");
                }
            }
        };
    }

    private ObjectMapper buildCacheObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder().allowIfSubType(Object.class).build(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        return mapper;
    }

    private String maskRedisUrl(String url) {
        if (url == null || url.isBlank()) {
            return "(not set)";
        }
        return url.replaceAll("(redis(?:s)?://[^:]+:)[^@]+(@.*)", "$1****$2");
    }
}
