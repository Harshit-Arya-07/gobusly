package com.busbooking.service;

import com.busbooking.dto.BookingRequestDto;
import com.busbooking.dto.BookingResponseDto;

import java.util.List;

public interface BookingService {
    BookingResponseDto createBooking(BookingRequestDto request);
    List<BookingResponseDto> getAllBookings();
    List<BookingResponseDto> getBookingsByUserId(String userId);
    void cancelBooking(String bookingId);
}
