package com.busbooking.service;

import java.util.List;

public interface SeatLockService {
    void lockSeats(String busId, List<Integer> seatNumbers, String ownerId);

    void validateSeatLocks(String busId, List<Integer> seatNumbers, String ownerId);

    void releaseSeatLocks(String busId, List<Integer> seatNumbers, String ownerId);
}