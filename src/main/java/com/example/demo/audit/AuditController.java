
package com.example.demo.audit;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@PreAuthorize("hasAnyRole('FACILITY_MANAGER','APPROVER','ADMIN')")
public class AuditController {

    private final AuditLogRepository auditRepository;

    public AuditController(AuditLogRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @GetMapping
    public List<AuditLog> getAllAuditLogs() {
        return auditRepository.findAllByOrderByCreatedAtDesc();
    }

    @GetMapping("/{entityType}/{entityId}")
    public List<AuditLog> getEntityHistory(
            @PathVariable String entityType,
            @PathVariable Long entityId) {

        return auditRepository
                .findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
                        entityType, entityId);
    }
}
