package com.busbooking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentHistoryResponseDto {
    private String id;
    private String userId;
    private String busId;
    private String busName;
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
