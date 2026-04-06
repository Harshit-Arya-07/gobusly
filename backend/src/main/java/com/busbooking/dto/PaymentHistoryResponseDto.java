package com.busbooking.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PaymentHistoryResponseDto {
    private String id;
    private String userId;
    private String busId;
    private List<Integer> seatNumbers;
    private Integer amountInRupees;
    private String orderId;
    private String paymentId;
    private String receipt;
    private String status;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
