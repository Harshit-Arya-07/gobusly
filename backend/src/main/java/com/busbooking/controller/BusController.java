package com.busbooking.controller;

import com.busbooking.dto.ApiMessageResponseDto;
import com.busbooking.dto.BusRequestDto;
import com.busbooking.dto.BusResponseDto;
import com.busbooking.service.BusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/buses")
@RequiredArgsConstructor
public class BusController {

    private final BusService busService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BusResponseDto> createBus(@Valid @RequestBody BusRequestDto request) {
        return ResponseEntity.ok(busService.createBus(request));
    }

    @GetMapping
    public ResponseEntity<List<BusResponseDto>> getAllBuses() {
        return ResponseEntity.ok(busService.getAllBuses());
    }

    @GetMapping("/search")
    public ResponseEntity<List<BusResponseDto>> searchBuses(
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(busService.searchBuses(source, destination, date));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BusResponseDto> updateBus(@PathVariable String id, @Valid @RequestBody BusRequestDto request) {
        return ResponseEntity.ok(busService.updateBus(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiMessageResponseDto> deleteBus(@PathVariable String id) {
        busService.deleteBus(id);
        return ResponseEntity.ok(new ApiMessageResponseDto("Bus deleted successfully"));
    }
}
