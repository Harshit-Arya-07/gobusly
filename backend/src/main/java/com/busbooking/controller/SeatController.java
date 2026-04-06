package com.busbooking.controller;

import com.busbooking.dto.SeatResponseDto;
import com.busbooking.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seats")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    @GetMapping("/{busId}")
    public ResponseEntity<List<SeatResponseDto>> getSeatsByBusId(@PathVariable String busId) {
        return ResponseEntity.ok(seatService.getSeatsByBusId(busId));
    }
}
