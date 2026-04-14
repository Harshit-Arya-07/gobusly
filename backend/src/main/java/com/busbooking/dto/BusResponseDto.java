package com.busbooking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusResponseDto {
    private String id;
    private String busNumber;
    private String busName;
    private String source;
    private String destination;
    private LocalDateTime time;
    private Integer totalSeats;
    private Integer fareInRupees;
    private Integer availableSeats;
}
