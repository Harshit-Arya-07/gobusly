package com.busbooking.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "seats")
@CompoundIndexes({
        @CompoundIndex(name = "bus_seat_unique", def = "{'busId': 1, 'seatNumber': 1}", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seat {

    @Id
    private String id;

    private Integer seatNumber;

    private Boolean isBooked;

    private String busId;
}
