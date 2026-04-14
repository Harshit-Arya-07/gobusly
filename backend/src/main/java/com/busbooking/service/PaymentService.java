package com.busbooking.service;

import com.busbooking.dto.PaymentOrderRequestDto;
import com.busbooking.dto.PaymentOrderResponseDto;
import com.busbooking.dto.PaymentFailureRequestDto;
import com.busbooking.dto.PaymentHistoryResponseDto;
import com.busbooking.dto.PaymentVerifyRequestDto;
import com.busbooking.dto.PageResponseDto;

public interface PaymentService {
    PaymentOrderResponseDto createOrder(PaymentOrderRequestDto request);
    void verifyPaymentSignature(PaymentVerifyRequestDto request);
    void markPaymentFailed(PaymentFailureRequestDto request);
    PageResponseDto<PaymentHistoryResponseDto> getPaymentHistoryByUserId(String userId, int page, int size);
    PageResponseDto<PaymentHistoryResponseDto> getAllPaymentHistory(int page, int size);
}
