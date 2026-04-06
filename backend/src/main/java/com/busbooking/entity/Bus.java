package com.busbooking.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "buses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bus {

    @Id
    private String id;

    @Indexed(unique = true)
    private String busNumber;

    private String source;

    private String destination;

    private LocalDateTime time;

    private Integer totalSeats;

    // Fare is stored in rupees per seat and managed by admin.
    private Integer fareInRupees;

    private String createdByAdminId;
}
