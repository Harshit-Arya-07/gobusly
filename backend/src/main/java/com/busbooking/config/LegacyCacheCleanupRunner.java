package com.busbooking.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class LegacyCacheCleanupRunner implements ApplicationRunner {

    private static final int DELETE_BATCH_SIZE = 500;

    private final StringRedisTemplate redisTemplate;

    @Value("${app.cache.cleanup-legacy-prefixes:true}")
    private boolean cleanupEnabled;

    @Value("${app.cache.cleanup-prefixes:v1::,v2::,v3::,v4::}")
    private String cleanupPrefixes;

    @Override
    public void run(ApplicationArguments args) {
        if (!cleanupEnabled) {
            log.info("Legacy cache cleanup is disabled");
            return;
        }

        try {
            RedisConnection connection = redisTemplate.getConnectionFactory() == null
                    ? null
                    : redisTemplate.getConnectionFactory().getConnection();
            if (connection == null) {
                log.warn("Skipping legacy cache cleanup: Redis connection unavailable");
                return;
            }
            connection.ping();

            long totalDeleted = 0;
            List<String> prefixes = Arrays.stream(cleanupPrefixes.split(","))
                    .map(String::trim)
                    .filter(prefix -> !prefix.isBlank())
                    .toList();

            for (String prefix : prefixes) {
                long deletedForPrefix = deleteByPrefix(prefix);
                totalDeleted += deletedForPrefix;
                if (deletedForPrefix > 0) {
                    log.info("Deleted {} legacy cache keys for prefix '{}'", deletedForPrefix, prefix);
                }
            }

            if (totalDeleted == 0) {
                log.info("Legacy cache cleanup completed: no keys found for configured prefixes");
            } else {
                log.info("Legacy cache cleanup completed: total keys deleted={}", totalDeleted);
            }

        } catch (Exception ex) {
            log.warn("Legacy cache cleanup skipped due to Redis error: {}", ex.getMessage());
        }
    }

    private long deleteByPrefix(String prefix) {
        String pattern = prefix + "*";
        long deleted = 0;

        List<String> keys = redisTemplate.execute((RedisConnection connection) -> {
            List<String> found = new ArrayList<>();
            ScanOptions options = ScanOptions.scanOptions()
                    .match(pattern)
                    .count(1000)
                    .build();

            try (Cursor<byte[]> cursor = connection.scan(options)) {
                while (cursor.hasNext()) {
                    found.add(new String(cursor.next(), StandardCharsets.UTF_8));
                }
            } catch (Exception scanEx) {
                log.warn("Scan failed for prefix '{}': {}", prefix, scanEx.getMessage());
            }

            return found;
        });

        if (keys == null || keys.isEmpty()) {
            return 0;
        }

        for (int start = 0; start < keys.size(); start += DELETE_BATCH_SIZE) {
            int end = Math.min(start + DELETE_BATCH_SIZE, keys.size());
            List<String> batch = keys.subList(start, end);
            Long removed = redisTemplate.delete(batch);
            if (removed != null) {
                deleted += removed;
            }
        }

        return deleted;
    }
}
