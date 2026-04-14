package com.busbooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class BusBookingSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(BusBookingSystemApplication.class, args);
    }
}
