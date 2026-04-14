package com.busbooking.service;

import com.busbooking.dto.AuthRequestDto;
import com.busbooking.dto.AuthResponseDto;
import com.busbooking.dto.ApiMessageResponseDto;
import com.busbooking.dto.ResendEmailOtpRequestDto;
import com.busbooking.dto.RegisterRequestDto;
import com.busbooking.dto.VerifyEmailOtpRequestDto;

public interface AuthService {
    ApiMessageResponseDto register(RegisterRequestDto request);
    AuthResponseDto login(AuthRequestDto request);
    AuthResponseDto verifyEmailOtp(VerifyEmailOtpRequestDto request);
    ApiMessageResponseDto resendEmailOtp(ResendEmailOtpRequestDto request);
}
