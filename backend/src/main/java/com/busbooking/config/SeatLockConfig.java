package com.busbooking.config;

import com.busbooking.service.InMemorySeatLockService;
import com.busbooking.service.RedisSeatLockService;
import com.busbooking.service.SeatLockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Chooses between Redis-backed and in-memory seat locking at startup.
 * <p>
 * If Redis is reachable, the app uses {@link RedisSeatLockService} for
 * distributed locking. Otherwise, it falls back to
 * {@link InMemorySeatLockService} so the app can still run locally
 * without Redis.
 * </p>
 */
@Configuration
@Slf4j
public class SeatLockConfig {

    @Value("${app.seat-lock.ttl-minutes:5}")
    private long seatLockTtlMinutes;

    @Bean
    @Primary
    public SeatLockService seatLockService(RedisConnectionFactory connectionFactory) {
        try {
            connectionFactory.getConnection().ping();
            log.info("Redis is available — using RedisSeatLockService for distributed seat locking");
            StringRedisTemplate redisTemplate = new StringRedisTemplate(connectionFactory);
            return new RedisSeatLockService(redisTemplate, seatLockTtlMinutes);
        } catch (Exception ex) {
            log.warn("Redis is unavailable — using InMemorySeatLockService (single-instance only). Reason: {}", ex.getMessage());
            return new InMemorySeatLockService(seatLockTtlMinutes);
        }
    }
}
