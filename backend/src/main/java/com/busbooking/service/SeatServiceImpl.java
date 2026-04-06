package com.busbooking.service;

import com.busbooking.dto.SeatResponseDto;
import com.busbooking.entity.Bus;
import com.busbooking.exception.ResourceNotFoundException;
import com.busbooking.repository.BusRepository;
import com.busbooking.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SeatServiceImpl implements SeatService {

    private final SeatRepository seatRepository;
    private final BusRepository busRepository;

    @Override
    public List<SeatResponseDto> getSeatsByBusId(String busId) {
        Bus bus = busRepository.findById(busId)
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with id: " + busId));

        return seatRepository.findByBusIdOrderBySeatNumberAsc(bus.getId())
                .stream()
                .map(seat -> SeatResponseDto.builder()
                        .id(seat.getId())
                        .seatNumber(seat.getSeatNumber())
                        .isBooked(seat.getIsBooked())
                        .busId(seat.getBusId())
                        .build())
                .toList();
    }
}
