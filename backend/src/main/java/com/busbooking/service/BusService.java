package com.busbooking.service;

import com.busbooking.dto.BusRequestDto;
import com.busbooking.dto.BusResponseDto;

import java.time.LocalDate;
import java.util.List;

public interface BusService {
    BusResponseDto createBus(BusRequestDto request);
    List<BusResponseDto> getAllBuses();
    List<BusResponseDto> searchBuses(String source, String destination, LocalDate travelDate);
    BusResponseDto updateBus(String id, BusRequestDto request);
    void deleteBus(String id);
}
