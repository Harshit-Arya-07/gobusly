package com.busbooking.service;

import com.busbooking.exception.InvalidBookingException;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory fallback implementation of {@link SeatLockService} used
 * when Redis is unavailable.
 * <p>
 * Suitable for single-instance deployments or local development.
 * For multi-instance production deployments, use {@link RedisSeatLockService}.
 * </p>
 */
@Slf4j
public class InMemorySeatLockService implements SeatLockService {

    private final Map<String, LockEntry> locks = new ConcurrentHashMap<>();
    private final long ttlMillis;

    public InMemorySeatLockService(long ttlMinutes) {
        this.ttlMillis = ttlMinutes * 60_000L;
        log.info("Using in-memory seat lock service (TTL={}min). For production, configure Redis.", ttlMinutes);
    }

    @Override
    public void lockSeats(String busId, List<Integer> seatNumbers, String ownerId) {
        List<Integer> normalizedSeats = normalizeSeatNumbers(seatNumbers);

        // Check all seats are available first
        for (Integer seat : normalizedSeats) {
            String key = buildKey(busId, seat);
            LockEntry existing = locks.get(key);
            if (existing != null && !existing.isExpired()) {
                throw new InvalidBookingException("Seat(s) already locked: " + normalizedSeats.stream()
                        .filter(s -> {
                            LockEntry e = locks.get(buildKey(busId, s));
                            return e != null && !e.isExpired();
                        }).toList());
            }
        }

        // Lock all seats
        Instant expiry = Instant.now().plusMillis(ttlMillis);
        for (Integer seat : normalizedSeats) {
            locks.put(buildKey(busId, seat), new LockEntry(ownerId, expiry));
        }
    }

    @Override
    public void validateSeatLocks(String busId, List<Integer> seatNumbers, String ownerId) {
        List<Integer> normalizedSeats = normalizeSeatNumbers(seatNumbers);
        List<Integer> unavailableSeats = new ArrayList<>();

        for (Integer seat : normalizedSeats) {
            String key = buildKey(busId, seat);
            LockEntry entry = locks.get(key);
            if (entry == null || entry.isExpired() || !Objects.equals(ownerId, entry.owner)) {
                unavailableSeats.add(seat);
            }
        }

        if (!unavailableSeats.isEmpty()) {
            throw new InvalidBookingException("Seat reservation expired or belongs to another session: " + unavailableSeats);
        }
    }

    @Override
    public void releaseSeatLocks(String busId, List<Integer> seatNumbers, String ownerId) {
        List<Integer> normalizedSeats = normalizeSeatNumbers(seatNumbers);
        for (Integer seat : normalizedSeats) {
            String key = buildKey(busId, seat);
            LockEntry entry = locks.get(key);
            if (entry != null && Objects.equals(ownerId, entry.owner)) {
                locks.remove(key);
            }
        }
    }

    private String buildKey(String busId, Integer seatNumber) {
        return "seat:" + busId + ":" + seatNumber;
    }

    private List<Integer> normalizeSeatNumbers(List<Integer> seatNumbers) {
        if (seatNumbers == null || seatNumbers.isEmpty()) {
            throw new InvalidBookingException("At least one seat must be selected");
        }
        return seatNumbers.stream().distinct().toList();
    }

    private record LockEntry(String owner, Instant expiry) {
        boolean isExpired() {
            return Instant.now().isAfter(expiry);
        }
    }
}
