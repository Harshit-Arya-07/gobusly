package com.busbooking.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BookingRequestDto {

    @NotNull
    private String userId;

    @NotNull
    private String busId;

    @NotEmpty
    private List<Integer> seatNumbers;
}
