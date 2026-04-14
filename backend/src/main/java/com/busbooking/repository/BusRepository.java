package com.busbooking.repository;

import com.busbooking.entity.Bus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface BusRepository extends MongoRepository<Bus, String> {
    List<Bus> findBySourceIgnoreCaseAndDestinationIgnoreCase(String source, String destination);

    /**
     * Case-insensitive regex search on source and destination with optional
     * date range filtering. Pushes the filtering to MongoDB instead of
     * loading the entire bus collection into memory.
     */
    @Query("{ " +
            "'source': { $regex: ?0, $options: 'i' }, " +
            "'destination': { $regex: ?1, $options: 'i' }, " +
            "'time': { $gte: ?2, $lt: ?3 } " +
            "}")
    List<Bus> searchBySourceAndDestinationAndDateRange(
            String sourcePattern,
            String destinationPattern,
            LocalDateTime dateStart,
            LocalDateTime dateEnd
    );

    /**
     * Case-insensitive regex search on source and destination without date filter.
     */
    @Query("{ " +
            "'source': { $regex: ?0, $options: 'i' }, " +
            "'destination': { $regex: ?1, $options: 'i' } " +
            "}")
    List<Bus> searchBySourceAndDestination(String sourcePattern, String destinationPattern);
}
