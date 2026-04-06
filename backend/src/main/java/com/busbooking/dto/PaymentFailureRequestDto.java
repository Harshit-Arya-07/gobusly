package com.busbooking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PaymentFailureRequestDto {

    @NotBlank
    private String razorpayOrderId;

    private String razorpayPaymentId;

    private String reason;
}
