package com.busbooking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BusRequestDto {

    @NotBlank
    private String busNumber;

    @NotBlank
    private String source;

    @NotBlank
    private String destination;

    @NotNull
    private LocalDateTime time;

    @NotNull
    @Min(1)
    private Integer totalSeats;

    @NotNull
    @Min(1)
    private Integer fareInRupees;
}
