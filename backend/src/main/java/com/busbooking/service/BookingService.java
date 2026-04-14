package com.busbooking.service;

import com.busbooking.dto.BookingRequestDto;
import com.busbooking.dto.BookingResponseDto;
import com.busbooking.dto.PageResponseDto;

public interface BookingService {
    BookingResponseDto createBooking(BookingRequestDto request);
    PageResponseDto<BookingResponseDto> getAllBookings(int page, int size);
    PageResponseDto<BookingResponseDto> getBookingsByUserId(String userId, int page, int size);
    void cancelBooking(String bookingId);
}
