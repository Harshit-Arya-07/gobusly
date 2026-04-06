package com.busbooking.repository;

import com.busbooking.entity.Booking;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface BookingRepository extends MongoRepository<Booking, String> {
    List<Booking> findByUserIdOrderByBookingTimeDesc(String userId);

    List<Booking> findAllByOrderByBookingTimeDesc();

    void deleteByBusId(String busId);
}
