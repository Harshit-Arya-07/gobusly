package com.busbooking.service;

import com.busbooking.dto.PaymentOrderRequestDto;
import com.busbooking.dto.PaymentOrderResponseDto;
import com.busbooking.dto.PaymentFailureRequestDto;
import com.busbooking.dto.PaymentHistoryResponseDto;
import com.busbooking.dto.PaymentVerifyRequestDto;

import java.util.List;

public interface PaymentService {
    PaymentOrderResponseDto createOrder(PaymentOrderRequestDto request);
    void verifyPaymentSignature(PaymentVerifyRequestDto request);
    void markPaymentFailed(PaymentFailureRequestDto request);
    List<PaymentHistoryResponseDto> getPaymentHistoryByUserId(String userId);
    List<PaymentHistoryResponseDto> getAllPaymentHistory();
}
