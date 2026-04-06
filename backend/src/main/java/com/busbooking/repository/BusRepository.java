package com.busbooking.repository;

import com.busbooking.entity.Bus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface BusRepository extends MongoRepository<Bus, String> {
    List<Bus> findBySourceIgnoreCaseAndDestinationIgnoreCase(String source, String destination);
}
