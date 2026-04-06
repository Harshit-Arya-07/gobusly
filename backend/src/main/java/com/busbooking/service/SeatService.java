package com.busbooking.service;

import com.busbooking.dto.SeatResponseDto;

import java.util.List;

public interface SeatService {
    List<SeatResponseDto> getSeatsByBusId(String busId);
}
