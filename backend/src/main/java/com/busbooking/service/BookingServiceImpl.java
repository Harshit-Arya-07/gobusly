package com.busbooking.service;

import com.busbooking.dto.BookingRequestDto;
import com.busbooking.dto.BookingResponseDto;
import com.busbooking.dto.PassengerDetailDto;
import com.busbooking.dto.PageResponseDto;
import com.busbooking.entity.*;
import com.busbooking.exception.InvalidBookingException;
import com.busbooking.exception.ResourceNotFoundException;
import com.busbooking.exception.UnauthorizedAccessException;
import com.busbooking.repository.BookingRepository;
import com.busbooking.repository.BusRepository;
import com.busbooking.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final BusRepository busRepository;
    private final SeatRepository seatRepository;
    private final MongoTemplate mongoTemplate;
    private final SeatLockService seatLockService;
    private final AuthenticatedUserService authenticatedUserService;

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "seatAvailability", key = "#request.busId"),
            @CacheEvict(value = "allBuses", allEntries = true),
            @CacheEvict(value = "busSearch", allEntries = true)
    })
    public BookingResponseDto createBooking(BookingRequestDto request) {
        User currentUser = authenticatedUserService.getCurrentUser();

        if (currentUser.getRole().equals(Role.ADMIN)) {
            throw new UnauthorizedAccessException("Admin users are not allowed to create bookings");
        }
        if (!currentUser.getId().equals(request.getUserId())) {
            throw new UnauthorizedAccessException("You can only create bookings for your own account");
        }

        Bus bus = busRepository.findById(request.getBusId())
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with id: " + request.getBusId()));

        List<Integer> requestedSeatNumbers = request.getSeatNumbers().stream().distinct().sorted().toList();
        List<PassengerDetail> passengerDetails = normalizePassengerDetails(request.getPassengerDetails(), requestedSeatNumbers);

        Booking existingBooking = bookingRepository
            .findTopByUserIdAndBusIdAndSeatNumbersAndStatusOrderByBookingTimeDesc(
                currentUser.getId(),
                bus.getId(),
                requestedSeatNumbers,
                BookingStatus.CONFIRMED
            )
            .orElse(null);

        if (existingBooking != null) {
            log.info("Returning existing confirmed booking (idempotent): id={}, userId={}, busId={}, seats={}",
                existingBooking.getId(), currentUser.getId(), bus.getId(), requestedSeatNumbers);
            return mapToDto(existingBooking);
        }

        List<Seat> seats = seatRepository.findByBusIdAndSeatNumberIn(bus.getId(), requestedSeatNumbers);

        if (seats.size() != requestedSeatNumbers.size()) {
            throw new InvalidBookingException("One or more selected seats do not exist for this bus");
        }

        seatLockService.validateSeatLocks(bus.getId(), requestedSeatNumbers, currentUser.getId());

        List<Integer> alreadyBooked = seats.stream()
                .filter(Seat::getIsBooked)
                .map(Seat::getSeatNumber)
                .toList();

        if (!alreadyBooked.isEmpty()) {
            throw new InvalidBookingException("Seats already booked: " + alreadyBooked);
        }

        updateSeatBookedState(seats, true);

        Booking booking = Booking.builder()
                .userId(currentUser.getId())
                .busId(bus.getId())
                .seatIds(seats.stream().map(Seat::getId).toList())
                .seatNumbers(seats.stream().map(Seat::getSeatNumber).sorted().toList())
                .passengerDetails(passengerDetails)
                .status(BookingStatus.CONFIRMED)
                .bookingTime(LocalDateTime.now())
                .build();

        Booking savedBooking = bookingRepository.save(booking);
        seatLockService.releaseSeatLocks(bus.getId(), requestedSeatNumbers, currentUser.getId());

        log.info("Booking created: id={}, userId={}, busId={}, seats={}",
                savedBooking.getId(), currentUser.getId(), bus.getId(), requestedSeatNumbers);

        return mapToDto(savedBooking);
    }

    @Override
    public PageResponseDto<BookingResponseDto> getAllBookings(int page, int size) {
        throw new UnauthorizedAccessException("Admin role can only add buses");
    }

    @Override
    public PageResponseDto<BookingResponseDto> getBookingsByUserId(String userId, int page, int size) {
        User currentUser = authenticatedUserService.getCurrentUser();
        if (!currentUser.getId().equals(userId)) {
            throw new UnauthorizedAccessException("You can only view your own bookings");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Booking> bookingPage = bookingRepository.findByUserIdOrderByBookingTimeDesc(userId, pageable);
        return buildPageResponse(bookingPage);
    }

    @Override
    @Transactional
    @CacheEvict(value = "seatAvailability", allEntries = true)
    public void cancelBooking(String bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        User currentUser = authenticatedUserService.getCurrentUser();
        if (!currentUser.getId().equals(booking.getUserId())) {
            throw new UnauthorizedAccessException("You can only cancel your own bookings");
        }

        if (BookingStatus.CANCELLED.equals(booking.getStatus())) {
            log.info("Booking already cancelled: id={}, userId={}, by={}",
                    bookingId, booking.getUserId(), currentUser.getId());
            return;
        }

        booking.setStatus(BookingStatus.CANCELLED);

        List<Seat> seatsToRelease = new ArrayList<>();
        if (booking.getSeatIds() != null && !booking.getSeatIds().isEmpty()) {
            seatsToRelease = seatRepository.findAllById(booking.getSeatIds());
        }

        if (seatsToRelease.isEmpty()
                && booking.getBusId() != null
                && booking.getSeatNumbers() != null
                && !booking.getSeatNumbers().isEmpty()) {
            seatsToRelease = seatRepository.findByBusIdAndSeatNumberIn(booking.getBusId(), booking.getSeatNumbers());
        }

        if (!seatsToRelease.isEmpty()) {
            updateSeatBookedState(seatsToRelease, false);
        }

        Query bookingQuery = Query.query(Criteria.where("_id").is(booking.getId()));
        Update bookingUpdate = new Update()
            .set("status", BookingStatus.CANCELLED)
            .set("bookingTime", booking.getBookingTime());
        mongoTemplate.updateFirst(bookingQuery, bookingUpdate, Booking.class);

        log.info("Booking cancelled: id={}, userId={}, by={}",
                bookingId, booking.getUserId(), currentUser.getId());
    }

    private BookingResponseDto mapToDto(Booking booking) {
        Bus bus = booking.getBusId() == null ? null : busRepository.findById(booking.getBusId()).orElse(null);
        return BookingResponseDto.builder()
                .id(booking.getId())
                .userId(booking.getUserId())
                .busId(booking.getBusId())
            .busName(bus == null ? null : bus.getBusName())
                .status(booking.getStatus() == null ? "CONFIRMED" : booking.getStatus().name())
                .bookingTime(booking.getBookingTime())
                .seatNumbers(booking.getSeatNumbers())
                .passengerDetails(mapPassengerDetails(booking.getPassengerDetails()))
                .build();
    }

    private List<PassengerDetail> normalizePassengerDetails(List<PassengerDetailDto> passengerDetails, List<Integer> seatNumbers) {
        if (passengerDetails == null || passengerDetails.isEmpty()) {
            throw new InvalidBookingException("Passenger details are required for each selected seat");
        }

        if (passengerDetails.size() != seatNumbers.size()) {
            throw new InvalidBookingException("Passenger details count must match selected seat count");
        }

        return IntStream.range(0, passengerDetails.size())
                .mapToObj(index -> {
                    PassengerDetailDto detail = passengerDetails.get(index);
                    if (detail == null || detail.getName() == null || detail.getName().isBlank()) {
                        throw new InvalidBookingException("Passenger name is required for seat " + seatNumbers.get(index));
                    }
                    if (detail.getAge() == null || detail.getAge() < 1) {
                        throw new InvalidBookingException("Passenger age is required for seat " + seatNumbers.get(index));
                    }
                    if (detail.getGender() == null || detail.getGender().isBlank()) {
                        throw new InvalidBookingException("Passenger gender is required for seat " + seatNumbers.get(index));
                    }

                    return PassengerDetail.builder()
                            .name(detail.getName().trim())
                            .age(detail.getAge())
                            .gender(detail.getGender().trim().toUpperCase())
                            .build();
                })
                .toList();
    }

    private List<PassengerDetailDto> mapPassengerDetails(List<PassengerDetail> passengerDetails) {
        if (passengerDetails == null) {
            return List.of();
        }

        return passengerDetails.stream()
                .map(detail -> PassengerDetailDto.builder()
                        .name(detail.getName())
                        .age(detail.getAge())
                        .gender(detail.getGender())
                        .build())
                .toList();
    }

    private PageResponseDto<BookingResponseDto> buildPageResponse(Page<Booking> bookingPage) {
        List<BookingResponseDto> content = bookingPage.getContent()
                .stream()
                .map(this::mapToDto)
                .toList();

        return PageResponseDto.<BookingResponseDto>builder()
                .content(content)
                .page(bookingPage.getNumber())
                .size(bookingPage.getSize())
                .totalElements(bookingPage.getTotalElements())
                .totalPages(bookingPage.getTotalPages())
                .last(bookingPage.isLast())
                .build();
    }

    private void updateSeatBookedState(List<Seat> seats, boolean isBooked) {
        if (seats == null || seats.isEmpty()) {
            return;
        }

        for (Seat seat : seats) {
            if (seat.getId() == null || seat.getId().isBlank()) {
                continue;
            }
            Query query = Query.query(Criteria.where("_id").is(seat.getId()));
            Update update = new Update().set("isBooked", isBooked);
            mongoTemplate.updateFirst(query, update, Seat.class);
        }
    }
}
