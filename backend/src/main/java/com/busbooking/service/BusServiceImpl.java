package com.busbooking.service;

import com.busbooking.dto.BusRequestDto;
import com.busbooking.dto.BusResponseDto;
import com.busbooking.entity.Bus;
import com.busbooking.entity.Role;
import com.busbooking.entity.Seat;
import com.busbooking.entity.User;
import com.busbooking.exception.InvalidBookingException;
import com.busbooking.exception.ResourceNotFoundException;
import com.busbooking.exception.UnauthorizedAccessException;
import com.busbooking.repository.BookingRepository;
import com.busbooking.repository.BusRepository;
import com.busbooking.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusServiceImpl implements BusService {

    private final BusRepository busRepository;
    private final SeatRepository seatRepository;
    private final BookingRepository bookingRepository;
    private final AuthenticatedUserService authenticatedUserService;

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "allBuses", allEntries = true),
            @CacheEvict(value = "busSearch", allEntries = true),
            @CacheEvict(value = "seatAvailability", allEntries = true)
    })
    public BusResponseDto createBus(BusRequestDto request) {
        User currentUser = getAdminUser();
        String busName = resolveBusName(request.getBusName(), request.getBusNumber());

        Bus bus = Bus.builder()
                .busNumber(request.getBusNumber())
            .busName(busName)
                .source(request.getSource())
                .destination(request.getDestination())
                .time(request.getTime())
                .totalSeats(request.getTotalSeats())
                .fareInRupees(request.getFareInRupees())
                .createdByAdminId(currentUser.getId())
                .build();

        Bus savedBus = busRepository.save(bus);
        log.info("Bus created: id={}, busNumber={}, by admin={}", savedBus.getId(), savedBus.getBusNumber(), currentUser.getId());

        List<Seat> seats = new ArrayList<>();
        for (int i = 1; i <= savedBus.getTotalSeats(); i++) {
            seats.add(Seat.builder()
                    .seatNumber(i)
                    .isBooked(false)
                    .busId(savedBus.getId())
                    .build());
        }
        seatRepository.saveAll(seats);

        return mapToDto(savedBus);
    }

    @Override
    @Cacheable(value = "allBuses", unless = "#result == null")
    public List<BusResponseDto> getAllBuses() {
        // Use a mutable list for cache serialization compatibility.
        return busRepository.findAll().stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "busSearch", key = "#source + '_' + #destination + '_' + #travelDate", unless = "#result == null")
    public List<BusResponseDto> searchBuses(String source, String destination, LocalDate travelDate) {
        String sourcePattern = escapeAndBuildPattern(source);
        String destinationPattern = escapeAndBuildPattern(destination);

        List<Bus> results;
        if (travelDate != null) {
            LocalDateTime dayStart = travelDate.atStartOfDay();
            LocalDateTime dayEnd = travelDate.atTime(LocalTime.MAX);
            results = busRepository.searchBySourceAndDestinationAndDateRange(
                    sourcePattern, destinationPattern, dayStart, dayEnd);
        } else {
            results = busRepository.searchBySourceAndDestination(sourcePattern, destinationPattern);
        }

        // Use a mutable list for cache serialization compatibility.
        return results.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "allBuses", allEntries = true),
            @CacheEvict(value = "busSearch", allEntries = true),
            @CacheEvict(value = "seatAvailability", allEntries = true)
    })
    public BusResponseDto updateBus(String id, BusRequestDto request) {
        throw new UnauthorizedAccessException("Admin role can only add buses");

        /*
        Bus bus = busRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with id: " + id));

        Integer previousTotalSeats = bus.getTotalSeats();

        bus.setBusNumber(request.getBusNumber());
        bus.setSource(request.getSource());
        bus.setDestination(request.getDestination());
        bus.setTime(request.getTime());
        bus.setTotalSeats(request.getTotalSeats());
        bus.setFareInRupees(request.getFareInRupees());

        Bus updatedBus = busRepository.save(bus);
        log.info("Bus updated: id={}, busNumber={}", updatedBus.getId(), updatedBus.getBusNumber());

        syncSeatsOnBusUpdate(updatedBus.getId(), previousTotalSeats, request.getTotalSeats());
        return mapToDto(updatedBus);
        */
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "allBuses", allEntries = true),
            @CacheEvict(value = "busSearch", allEntries = true),
            @CacheEvict(value = "seatAvailability", allEntries = true)
    })
    public void deleteBus(String id) {
        User currentUser = getAdminUser();

        Bus bus = busRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with id: " + id));

        if (bus.getCreatedByAdminId() == null || bus.getCreatedByAdminId().isBlank()) {
            throw new UnauthorizedAccessException("Bus owner information is missing; deletion is restricted");
        }

        if (!bus.getCreatedByAdminId().equals(currentUser.getId())) {
            throw new UnauthorizedAccessException("You can only delete buses created by your own admin account");
        }

        bookingRepository.deleteByBusId(id);
        seatRepository.deleteByBusId(id);
        busRepository.delete(bus);
        log.info("Bus deleted: id={}, busNumber={}, by admin={}", id, bus.getBusNumber(), currentUser.getId());
    }

    private BusResponseDto mapToDto(Bus bus) {
        long bookedCount = seatRepository.countByBusIdAndIsBooked(bus.getId(), true);
        int available = bus.getTotalSeats() - (int) bookedCount;
        return BusResponseDto.builder()
                .id(bus.getId())
                .busNumber(bus.getBusNumber())
                .busName(resolveBusName(bus.getBusName(), bus.getBusNumber()))
                .source(bus.getSource())
                .destination(bus.getDestination())
                .time(bus.getTime())
                .totalSeats(bus.getTotalSeats())
                .fareInRupees(bus.getFareInRupees())
                .availableSeats(available)
                .build();
    }

    private String resolveBusName(String busName, String busNumber) {
        if (busName != null && !busName.isBlank()) {
            return busName.trim();
        }

        return busNumber == null ? null : busNumber.trim();
    }

    private void syncSeatsOnBusUpdate(String busId, Integer previousTotalSeats, Integer updatedTotalSeats) {
        if (previousTotalSeats.equals(updatedTotalSeats)) {
            return;
        }

        if (updatedTotalSeats > previousTotalSeats) {
            List<Seat> newSeats = new ArrayList<>();
            for (int seatNumber = previousTotalSeats + 1; seatNumber <= updatedTotalSeats; seatNumber++) {
                newSeats.add(Seat.builder()
                        .seatNumber(seatNumber)
                        .isBooked(false)
                        .busId(busId)
                        .build());
            }
            seatRepository.saveAll(newSeats);
            log.info("Added {} new seats to bus {}", newSeats.size(), busId);
            return;
        }

        // Use targeted query instead of fetching all seats and filtering in Java
        List<Seat> removableSeats = seatRepository.findByBusIdAndSeatNumberGreaterThan(busId, updatedTotalSeats);

        boolean hasBookedSeatsToRemove = removableSeats.stream().anyMatch(Seat::getIsBooked);
        if (hasBookedSeatsToRemove) {
            throw new InvalidBookingException("Cannot reduce total seats because one or more higher seat numbers are already booked");
        }

        if (!removableSeats.isEmpty()) {
            seatRepository.deleteAll(removableSeats);
            log.info("Removed {} seats from bus {}", removableSeats.size(), busId);
        }
    }

    /**
     * Escapes user input for safe use in MongoDB regex and wraps it
     * as a partial-match pattern. Returns {@code ".*"} for empty input
     * (matches everything).
     */
    private String escapeAndBuildPattern(String value) {
        if (value == null || value.trim().isEmpty()) {
            return ".*";
        }
        return ".*" + Pattern.quote(value.trim()) + ".*";
    }

    /**
     * Returns the current user after verifying they have the ADMIN role.
     */
    private User getAdminUser() {
        User user = authenticatedUserService.getCurrentUser();
        if (!Role.ADMIN.equals(user.getRole())) {
            throw new UnauthorizedAccessException("Admin access required");
        }
        return user;
    }
}
