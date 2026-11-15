
package com.example.demo.maintenance;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    public MaintenanceController(MaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('STUDENT','FACILITY_MANAGER','ADMIN')")
    public MaintenanceTicket createTicket(
            @Valid @RequestBody TicketRequest request,
            Authentication authentication) {

        return maintenanceService.createTicket(
            request, authentication.getName()
        );
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('FACILITY_MANAGER','APPROVER','ADMIN')")
    public List<MaintenanceTicket> getAllTickets() {
        return maintenanceService.getAllTickets();
    }

    @GetMapping("/assigned")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public List<MaintenanceTicket> getAssignedTickets(
            Authentication authentication) {

        return maintenanceService.getMyAssignedTickets(
            authentication.getName()
        );
    }

    @PostMapping("/{id}/triage")
    @PreAuthorize("hasAnyRole('FACILITY_MANAGER','ADMIN')")
    public MaintenanceTicket triage(
            @PathVariable Long id,
            Authentication authentication) {

        return maintenanceService.triageTicket(
            id, authentication.getName()
        );
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('FACILITY_MANAGER','ADMIN')")
    public MaintenanceTicket assign(
            @PathVariable Long id,
            @Valid @RequestBody AssignmentRequest request,
            Authentication authentication) {

        return maintenanceService.assignTechnician(
            id, request.getTechnicianId(), authentication.getName()
        );
    }

    @PostMapping("/{id}/inspect")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public MaintenanceTicket inspect(
            @PathVariable Long id,
            @Valid @RequestBody NotesRequest request,
            Authentication authentication) {

        return maintenanceService.inspectTicket(
            id, request.getNotes(), authentication.getName()
        );
    }

    @PostMapping("/{id}/repair")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public MaintenanceTicket repair(
            @PathVariable Long id,
            @Valid @RequestBody NotesRequest request,
            Authentication authentication) {

        return maintenanceService.submitRepair(
            id, request.getNotes(), authentication.getName()
        );
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('APPROVER','ADMIN')")
    public MaintenanceTicket approve(
            @PathVariable Long id,
            @Valid @RequestBody NotesRequest request,
            Authentication authentication) {

        return maintenanceService.approveRepair(
            id, request.getNotes(), authentication.getName()
        );
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('APPROVER','ADMIN')")
    public MaintenanceTicket reject(
            @PathVariable Long id,
            @Valid @RequestBody NotesRequest request,
            Authentication authentication) {

        return maintenanceService.rejectRepair(
            id, request.getNotes(), authentication.getName()
        );
    }
}
