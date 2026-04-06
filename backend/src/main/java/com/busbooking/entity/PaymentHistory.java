package com.busbooking.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "payment_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentHistory {

    @Id
    private String id;

    private String userId;

    private String busId;

    @Builder.Default
    private List<Integer> seatNumbers = new ArrayList<>();

    private Integer amountInRupees;

    private String orderId;

    private String paymentId;

    private String receipt;

    private PaymentStatus status;

    private String failureReason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
