package com.busbooking.controller;

import com.busbooking.dto.AuthRequestDto;
import com.busbooking.dto.AuthResponseDto;
import com.busbooking.dto.ApiMessageResponseDto;
import com.busbooking.dto.ResendEmailOtpRequestDto;
import com.busbooking.dto.RegisterRequestDto;
import com.busbooking.dto.VerifyEmailOtpRequestDto;
import com.busbooking.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiMessageResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody AuthRequestDto request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<AuthResponseDto> verifyEmail(@Valid @RequestBody VerifyEmailOtpRequestDto request) {
        return ResponseEntity.ok(authService.verifyEmailOtp(request));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<ApiMessageResponseDto> resendOtp(@Valid @RequestBody ResendEmailOtpRequestDto request) {
        return ResponseEntity.ok(authService.resendEmailOtp(request));
    }
}
