
package com.example.demo.maintenance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface MaintenanceTicketRepository
        extends JpaRepository<MaintenanceTicket, Long> {

    Optional<MaintenanceTicket> findByRequestKey(String requestKey);

    List<MaintenanceTicket> findAllByOrderByCreatedAtDesc();

    List<MaintenanceTicket> findByAssignedTechnicianId(Long technicianId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM MaintenanceTicket t WHERE t.id = :id")
    Optional<MaintenanceTicket> findByIdForUpdate(@Param("id") Long id);
}
