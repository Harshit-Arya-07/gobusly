package com.busbooking.repository;

import com.busbooking.entity.Seat;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SeatRepository extends MongoRepository<Seat, String> {
    List<Seat> findByBusIdOrderBySeatNumberAsc(String busId);

    List<Seat> findByBusIdAndSeatNumberIn(String busId, List<Integer> seatNumbers);

    void deleteByBusId(String busId);
}
