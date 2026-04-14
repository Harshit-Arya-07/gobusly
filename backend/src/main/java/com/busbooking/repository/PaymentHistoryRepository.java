package com.busbooking.repository;

import com.busbooking.entity.PaymentHistory;
import com.busbooking.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentHistoryRepository extends MongoRepository<PaymentHistory, String> {
    Optional<PaymentHistory> findByOrderId(String orderId);

    List<PaymentHistory> findByOrderIdOrderByUpdatedAtDesc(String orderId);

    Page<PaymentHistory> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    Page<PaymentHistory> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Optional<PaymentHistory> findByUserIdAndBusIdAndSeatNumbersAndStatus(
            String userId, String busId, List<Integer> seatNumbers, PaymentStatus status);
}
