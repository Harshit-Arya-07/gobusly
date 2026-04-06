package com.busbooking.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentOrderResponseDto {
    private String keyId;
    private String orderId;
    private long amount;
    private String currency;
    private String receipt;
}
