package com.busbooking.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SeatResponseDto {
    private String id;
    private Integer seatNumber;
    private Boolean isBooked;
    private String busId;
}
