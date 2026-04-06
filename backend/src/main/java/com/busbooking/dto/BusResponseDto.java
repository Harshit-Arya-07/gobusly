package com.busbooking.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BusResponseDto {
    private String id;
    private String busNumber;
    private String source;
    private String destination;
    private LocalDateTime time;
    private Integer totalSeats;
    private Integer fareInRupees;
}
