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
import com.busbooking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BusServiceImpl implements BusService {

    private final BusRepository busRepository;
    private final SeatRepository seatRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public BusResponseDto createBus(BusRequestDto request) {
        User currentUser = getCurrentUser();

        Bus bus = Bus.builder()
                .busNumber(request.getBusNumber())
                .source(request.getSource())
                .destination(request.getDestination())
                .time(request.getTime())
                .totalSeats(request.getTotalSeats())
                .fareInRupees(request.getFareInRupees())
            .createdByAdminId(currentUser.getId())
                .build();

        Bus savedBus = busRepository.save(bus);

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
    public List<BusResponseDto> getAllBuses() {
        return busRepository.findAll().stream().map(this::mapToDto).toList();
    }

    @Override
        public List<BusResponseDto> searchBuses(String source, String destination, LocalDate travelDate) {
        String normalizedSource = normalize(source);
        String normalizedDestination = normalize(destination);

        return busRepository.findAll().stream()
            .filter(bus -> normalizedSource.isEmpty()
                || normalize(bus.getSource()).contains(normalizedSource))
            .filter(bus -> normalizedDestination.isEmpty()
                || normalize(bus.getDestination()).contains(normalizedDestination))
            .filter(bus -> travelDate == null || bus.getTime().toLocalDate().equals(travelDate))
                .map(this::mapToDto)
                .toList();
    }

    @Override
    @Transactional
    public BusResponseDto updateBus(String id, BusRequestDto request) {
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
        syncSeatsOnBusUpdate(updatedBus.getId(), previousTotalSeats, request.getTotalSeats());
        return mapToDto(updatedBus);
    }

    @Override
    @Transactional
    public void deleteBus(String id) {
        User currentUser = getCurrentUser();

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
    }

    private BusResponseDto mapToDto(Bus bus) {
        return BusResponseDto.builder()
                .id(bus.getId())
                .busNumber(bus.getBusNumber())
                .source(bus.getSource())
                .destination(bus.getDestination())
                .time(bus.getTime())
                .totalSeats(bus.getTotalSeats())
                .fareInRupees(bus.getFareInRupees())
                .build();
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
            return;
        }

        List<Seat> seats = seatRepository.findByBusIdOrderBySeatNumberAsc(busId);
        List<Seat> removableSeats = seats.stream()
                .filter(seat -> seat.getSeatNumber() > updatedTotalSeats)
                .toList();

        boolean hasBookedSeatsToRemove = removableSeats.stream().anyMatch(Seat::getIsBooked);
        if (hasBookedSeatsToRemove) {
            throw new InvalidBookingException("Cannot reduce total seats because one or more higher seat numbers are already booked");
        }

        if (!removableSeats.isEmpty()) {
            seatRepository.deleteAll(removableSeats);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new UnauthorizedAccessException("Unauthenticated access");
        }

        String email;
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            email = userDetails.getUsername();
        } else {
            email = authentication.getName();
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedAccessException("Current user not found"));

        if (!Role.ADMIN.equals(user.getRole())) {
            throw new UnauthorizedAccessException("Admin access required");
        }

        return user;
    }
}
