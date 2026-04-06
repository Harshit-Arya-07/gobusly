package com.busbooking.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    private String id;

    private String userId;

    private String busId;

    @Builder.Default
    private List<String> seatIds = new ArrayList<>();

    @Builder.Default
    private List<Integer> seatNumbers = new ArrayList<>();

    private BookingStatus status;

    private LocalDateTime bookingTime;
}
