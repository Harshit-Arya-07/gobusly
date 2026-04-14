package com.busbooking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponseDto {
    private String id;
    private String userId;
    private String busId;
    private String busName;
    private String status;
    private LocalDateTime bookingTime;
    private List<Integer> seatNumbers;
    private List<PassengerDetailDto> passengerDetails;
}
