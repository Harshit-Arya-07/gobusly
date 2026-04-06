package com.busbooking.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponseDto {
    private String token;
    private String userId;
    private String name;
    private String email;
    private String role;
}
