package com.busbooking.controller;

import com.busbooking.dto.ApiMessageResponseDto;
import com.busbooking.dto.PageResponseDto;
import com.busbooking.dto.PaymentFailureRequestDto;
import com.busbooking.dto.PaymentHistoryResponseDto;
import com.busbooking.dto.PaymentOrderRequestDto;
import com.busbooking.dto.PaymentOrderResponseDto;
import com.busbooking.dto.PaymentVerifyRequestDto;
import com.busbooking.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/order")
    public ResponseEntity<PaymentOrderResponseDto> createOrder(@Valid @RequestBody PaymentOrderRequestDto request) {
        return ResponseEntity.ok(paymentService.createOrder(request));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiMessageResponseDto> verify(@Valid @RequestBody PaymentVerifyRequestDto request) {
        paymentService.verifyPaymentSignature(request);
        return ResponseEntity.ok(new ApiMessageResponseDto("Payment verified successfully"));
    }

    @PostMapping("/fail")
    public ResponseEntity<ApiMessageResponseDto> markFailure(@Valid @RequestBody PaymentFailureRequestDto request) {
        paymentService.markPaymentFailed(request);
        return ResponseEntity.ok(new ApiMessageResponseDto("Payment failure recorded"));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<PageResponseDto<PaymentHistoryResponseDto>> getPaymentHistoryByUserId(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(paymentService.getPaymentHistoryByUserId(userId, page, size));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponseDto<PaymentHistoryResponseDto>> getAllPaymentHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(paymentService.getAllPaymentHistory(page, size));
    }
}
