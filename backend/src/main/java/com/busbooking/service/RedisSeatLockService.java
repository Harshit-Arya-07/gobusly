package com.busbooking.service;

import com.busbooking.exception.InvalidBookingException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Redis-backed distributed seat locking using Lua scripts for atomicity.
 * <p>
 * This bean is created by {@code SeatLockConfig} — not component-scanned.
 * </p>
 */
public class RedisSeatLockService implements SeatLockService {

    private static final DefaultRedisScript<Long> ACQUIRE_LOCK_SCRIPT = new DefaultRedisScript<>(
            "local owner = ARGV[1] " +
                    "local ttl = tonumber(ARGV[2]) " +
                    "for i, key in ipairs(KEYS) do " +
                    "  if redis.call('EXISTS', key) == 1 then return 0 end " +
                    "end " +
                    "for i, key in ipairs(KEYS) do " +
                    "  redis.call('SET', key, owner, 'PX', ttl) " +
                    "end " +
                    "return 1",
            Long.class
    );

    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT = new DefaultRedisScript<>(
            "local owner = ARGV[1] " +
                    "local removed = 0 " +
                    "for i, key in ipairs(KEYS) do " +
                    "  if redis.call('GET', key) == owner then " +
                    "    redis.call('DEL', key) " +
                    "    removed = removed + 1 " +
                    "  end " +
                    "end " +
                    "return removed",
            Long.class
    );

    static {
        ACQUIRE_LOCK_SCRIPT.setResultType(Long.class);
        RELEASE_LOCK_SCRIPT.setResultType(Long.class);
    }

    private final StringRedisTemplate redisTemplate;
    private final long seatLockTtlMinutes;

    public RedisSeatLockService(StringRedisTemplate redisTemplate, long seatLockTtlMinutes) {
        this.redisTemplate = redisTemplate;
        this.seatLockTtlMinutes = seatLockTtlMinutes;
    }

    @Override
    public void lockSeats(String busId, List<Integer> seatNumbers, String ownerId) {
        List<Integer> normalizedSeats = normalizeSeatNumbers(seatNumbers);
        List<String> keys = buildKeys(busId, normalizedSeats);
        Long locked = redisTemplate.execute(
                ACQUIRE_LOCK_SCRIPT,
                keys,
                ownerId,
                String.valueOf(Duration.ofMinutes(seatLockTtlMinutes).toMillis())
        );

        if (!Long.valueOf(1L).equals(locked)) {
            throw new InvalidBookingException(buildLockedSeatMessage(busId, normalizedSeats));
        }
    }

    @Override
    public void validateSeatLocks(String busId, List<Integer> seatNumbers, String ownerId) {
        List<Integer> normalizedSeats = normalizeSeatNumbers(seatNumbers);
        List<String> keys = buildKeys(busId, normalizedSeats);
        List<String> currentOwners = redisTemplate.opsForValue().multiGet(keys);

        if (currentOwners == null || currentOwners.size() != normalizedSeats.size()) {
            throw new InvalidBookingException("Seat reservation expired. Please reselect your seats.");
        }

        List<Integer> unavailableSeats = new ArrayList<>();
        for (int index = 0; index < normalizedSeats.size(); index++) {
            String currentOwner = currentOwners.get(index);
            if (!Objects.equals(ownerId, currentOwner)) {
                unavailableSeats.add(normalizedSeats.get(index));
            }
        }

        if (!unavailableSeats.isEmpty()) {
            throw new InvalidBookingException("Seat reservation expired or belongs to another session: " + unavailableSeats);
        }
    }

    @Override
    public void releaseSeatLocks(String busId, List<Integer> seatNumbers, String ownerId) {
        List<Integer> normalizedSeats = normalizeSeatNumbers(seatNumbers);
        List<String> keys = buildKeys(busId, normalizedSeats);
        redisTemplate.execute(RELEASE_LOCK_SCRIPT, keys, ownerId);
    }

    private List<String> buildKeys(String busId, List<Integer> seatNumbers) {
        return seatNumbers.stream()
                .map(seatNumber -> String.format("seat:%s:%s", busId, seatNumber))
                .toList();
    }

    private List<Integer> normalizeSeatNumbers(List<Integer> seatNumbers) {
        if (seatNumbers == null || seatNumbers.isEmpty()) {
            throw new InvalidBookingException("At least one seat must be selected");
        }

        return seatNumbers.stream().distinct().toList();
    }

    private String buildLockedSeatMessage(String busId, List<Integer> seatNumbers) {
        List<String> keys = buildKeys(busId, seatNumbers);
        List<String> currentOwners = redisTemplate.opsForValue().multiGet(keys);
        if (currentOwners == null) {
            return "One or more selected seats are temporarily unavailable";
        }

        List<Integer> lockedSeats = new ArrayList<>();
        for (int index = 0; index < seatNumbers.size(); index++) {
            if (currentOwners.size() > index && currentOwners.get(index) != null) {
                lockedSeats.add(seatNumbers.get(index));
            }
        }

        if (lockedSeats.isEmpty()) {
            return "One or more selected seats are temporarily unavailable";
        }

        return "Seat(s) already locked: " + lockedSeats;
    }
}