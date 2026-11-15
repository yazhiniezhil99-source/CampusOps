
package com.example.demo.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByRequestKey(String requestKey);

    @Query("""
        SELECT COUNT(b) > 0
        FROM Booking b
        WHERE b.facility.id = :facilityId
        AND b.status IN :statuses
        AND b.startTime < :endTime
        AND b.endTime > :startTime
    """)
    boolean hasOverlappingBooking(
        @Param("facilityId") Long facilityId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime,
        @Param("statuses") List<BookingStatus> statuses
    );

    List<Booking> findByRequestedById(Long userId);

    List<Booking> findByFacilityId(Long facilityId);
}
