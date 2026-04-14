package com.busbooking.repository;

import com.busbooking.entity.Booking;
import com.busbooking.entity.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends MongoRepository<Booking, String> {
    Page<Booking> findByUserIdOrderByBookingTimeDesc(String userId, Pageable pageable);

    Page<Booking> findAllByOrderByBookingTimeDesc(Pageable pageable);

    Optional<Booking> findTopByUserIdAndBusIdAndSeatNumbersAndStatusOrderByBookingTimeDesc(
            String userId,
            String busId,
            List<Integer> seatNumbers,
            BookingStatus status
    );

    void deleteByBusId(String busId);
}
