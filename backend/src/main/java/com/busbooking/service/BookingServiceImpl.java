package com.busbooking.service;

import com.busbooking.dto.BookingRequestDto;
import com.busbooking.dto.BookingResponseDto;
import com.busbooking.entity.*;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final BusRepository busRepository;
    private final SeatRepository seatRepository;

    @Override
    @Transactional
    public BookingResponseDto createBooking(BookingRequestDto request) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole().equals(Role.ADMIN)) {
            throw new UnauthorizedAccessException("Admin users are not allowed to create bookings");
        }
        if (!currentUser.getId().equals(request.getUserId())) {
            throw new UnauthorizedAccessException("You can only create bookings for your own account");
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        Bus bus = busRepository.findById(request.getBusId())
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with id: " + request.getBusId()));

        List<Integer> requestedSeatNumbers = request.getSeatNumbers().stream().distinct().toList();
        List<Seat> seats = seatRepository.findByBusIdAndSeatNumberIn(bus.getId(), requestedSeatNumbers);

        if (seats.size() != requestedSeatNumbers.size()) {
            throw new InvalidBookingException("One or more selected seats do not exist for this bus");
        }

        List<Integer> alreadyBooked = seats.stream()
                .filter(Seat::getIsBooked)
                .map(Seat::getSeatNumber)
                .toList();

        if (!alreadyBooked.isEmpty()) {
            throw new InvalidBookingException("Seats already booked: " + alreadyBooked);
        }

        seats.forEach(seat -> seat.setIsBooked(true));
        seatRepository.saveAll(seats);

        Booking booking = Booking.builder()
            .userId(user.getId())
            .busId(bus.getId())
            .seatIds(seats.stream().map(Seat::getId).toList())
            .seatNumbers(seats.stream().map(Seat::getSeatNumber).sorted().toList())
                .status(BookingStatus.CONFIRMED)
                .bookingTime(LocalDateTime.now())
                .build();

        Booking savedBooking = bookingRepository.save(booking);
        return mapToDto(savedBooking);
    }

    @Override
    public List<BookingResponseDto> getAllBookings() {
        User currentUser = getCurrentUser();
        if (!currentUser.getRole().equals(Role.ADMIN)) {
            throw new UnauthorizedAccessException("Admin access required");
        }

        return bookingRepository.findAllByOrderByBookingTimeDesc()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public List<BookingResponseDto> getBookingsByUserId(String userId) {
        User currentUser = getCurrentUser();
        if (!currentUser.getRole().equals(Role.ADMIN) && !currentUser.getId().equals(userId)) {
            throw new UnauthorizedAccessException("You can only view your own bookings");
        }

        return bookingRepository.findByUserIdOrderByBookingTimeDesc(userId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional
    public void cancelBooking(String bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        User currentUser = getCurrentUser();
        if (!currentUser.getRole().equals(Role.ADMIN) && !currentUser.getId().equals(booking.getUserId())) {
            throw new UnauthorizedAccessException("You can only cancel your own bookings");
        }

        if (booking.getStatus().equals(BookingStatus.CANCELLED)) {
            throw new InvalidBookingException("Booking is already cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
    List<Seat> seats = seatRepository.findAllById(booking.getSeatIds());
    seats.forEach(seat -> seat.setIsBooked(false));
    seatRepository.saveAll(seats);
        bookingRepository.save(booking);
    }

    private BookingResponseDto mapToDto(Booking booking) {
        return BookingResponseDto.builder()
                .id(booking.getId())
        .userId(booking.getUserId())
        .busId(booking.getBusId())
                .status(booking.getStatus().name())
                .bookingTime(booking.getBookingTime())
        .seatNumbers(booking.getSeatNumbers())
                .build();
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedAccessException("Unauthenticated access");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UnauthorizedAccessException("Current user not found"));
    }
}
