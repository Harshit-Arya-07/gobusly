package com.busbooking.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class BookingResponseDto {
    private String id;
    private String userId;
    private String busId;
    private String status;
    private LocalDateTime bookingTime;
    private List<Integer> seatNumbers;
}
