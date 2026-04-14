package com.busbooking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResendEmailOtpRequestDto {

    @Email
    @NotBlank
    private String email;
}