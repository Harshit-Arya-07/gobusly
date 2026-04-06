package com.busbooking.repository;

import com.busbooking.entity.PaymentHistory;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentHistoryRepository extends MongoRepository<PaymentHistory, String> {
    Optional<PaymentHistory> findByOrderId(String orderId);

    List<PaymentHistory> findByUserIdOrderByCreatedAtDesc(String userId);

    List<PaymentHistory> findAllByOrderByCreatedAtDesc();
}
