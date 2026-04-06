package com.busbooking.service;

import com.busbooking.dto.AuthRequestDto;
import com.busbooking.dto.AuthResponseDto;
import com.busbooking.dto.RegisterRequestDto;

public interface AuthService {
    AuthResponseDto register(RegisterRequestDto request);
    AuthResponseDto login(AuthRequestDto request);
}
