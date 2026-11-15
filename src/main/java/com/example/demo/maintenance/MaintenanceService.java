
package com.example.demo.maintenance;

import com.example.demo.audit.AuditService;
import com.example.demo.facility.Facility;
import com.example.demo.facility.FacilityRepository;
import com.example.demo.user.AppUser;
import com.example.demo.user.AppUserRepository;
import com.example.demo.user.Role;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class MaintenanceService {

    private final MaintenanceTicketRepository ticketRepository;
    private final FacilityRepository facilityRepository;
    private final AppUserRepository userRepository;
    private final AuditService auditService;

    public MaintenanceService(
            MaintenanceTicketRepository ticketRepository,
            FacilityRepository facilityRepository,
            AppUserRepository userRepository,
            AuditService auditService) {
        this.ticketRepository = ticketRepository;
        this.facilityRepository = facilityRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional
    public MaintenanceTicket createTicket(
            TicketRequest request, String username) {

        var existing = ticketRepository.findByRequestKey(
            request.getRequestKey()
        );

        if (existing.isPresent()) {
            MaintenanceTicket previous = existing.get();

            if (!previous.getReportedBy().getUsername().equals(username)) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Request key already belongs to another user"
                );
            }

            return previous;
        }

        AppUser reporter = getUser(username);

        Facility facility = facilityRepository.findById(
            request.getFacilityId()
        ).orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Facility not found"
        ));

        if (!facility.isActive()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Facility is inactive"
            );
        }

        MaintenanceTicket ticket = new MaintenanceTicket();
        ticket.setRequestKey(request.getRequestKey());
        ticket.setFacility(facility);
        ticket.setReportedBy(reporter);
        ticket.setTitle(request.getTitle());
        ticket.setDescription(request.getDescription());

        String description = request.getDescription().toLowerCase();

        if (description.contains("fire") ||
            description.contains("electric shock") ||
            description.contains("gas leak") ||
            description.contains("flood") ||
            description.contains("injury")) {

            ticket.setPriority("CRITICAL");
            ticket.setTriageReason(
                "Critical safety keyword detected in the report"
            );

        } else if (description.contains("broken") ||
                   description.contains("leak") ||
                   description.contains("failure") ||
                   description.contains("not working")) {

            ticket.setPriority("HIGH");
            ticket.setTriageReason(
                "Equipment failure or service disruption keyword detected"
            );

        } else {
            ticket.setPriority("NORMAL");
            ticket.setTriageReason(
                "No critical safety or major failure keyword detected"
            );
        }

        ticket.setStatus(TicketStatus.OPEN);

        MaintenanceTicket saved = ticketRepository.saveAndFlush(ticket);

        auditService.record(
            username,
            "TICKET_CREATED",
            "MAINTENANCE_TICKET",
            saved.getId(),
            "Priority: " + saved.getPriority()
                + "; Reason: " + saved.getTriageReason()
        );

        return saved;
    }

    @Transactional
    public MaintenanceTicket assignTechnician(
            Long ticketId, Long technicianId, String username) {

        MaintenanceTicket ticket = getTicketForUpdate(ticketId);

        if (ticket.getStatus() != TicketStatus.TRIAGED &&
            ticket.getStatus() != TicketStatus.OPEN &&
            ticket.getStatus() != TicketStatus.REJECTED) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Ticket cannot be assigned in its current state"
            );
        }

        AppUser technician = userRepository.findById(technicianId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Technician not found"
            ));

        if (!technician.isActive() || technician.getRole() != Role.TECHNICIAN) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Invalid technician account"
            );
        }

        ticket.setAssignedTechnician(technician);
        ticket.setStatus(TicketStatus.ASSIGNED);

        auditService.record(
            username,
            "TECHNICIAN_ASSIGNED",
            "MAINTENANCE_TICKET",
            ticketId,
            "Assigned to " + technician.getUsername()
        );

        return ticket;
    }

    @Transactional
    public MaintenanceTicket inspectTicket(
            Long ticketId, String notes, String username) {

        MaintenanceTicket ticket = getTicketForUpdate(ticketId);
        requireAssignedTechnician(ticket, username);

        if (ticket.getStatus() != TicketStatus.ASSIGNED) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Ticket is not ready for inspection"
            );
        }

        if (notes == null || notes.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Inspection notes are required"
            );
        }

        ticket.setInspectionNotes(notes);
        ticket.setStatus(TicketStatus.INSPECTION);

        auditService.record(
            username,
            "INSPECTION_COMPLETED",
            "MAINTENANCE_TICKET",
            ticketId,
            notes
        );

        return ticket;
    }

    @Transactional
    public MaintenanceTicket submitRepair(
            Long ticketId, String notes, String username) {

        MaintenanceTicket ticket = getTicketForUpdate(ticketId);
        requireAssignedTechnician(ticket, username);

        if (ticket.getStatus() != TicketStatus.INSPECTION) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Inspection must be completed first"
            );
        }

        if (notes == null || notes.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Repair notes are required"
            );
        }

        ticket.setRepairNotes(notes);
        ticket.setStatus(TicketStatus.AWAITING_APPROVAL);

        auditService.record(
            username,
            "REPAIR_SUBMITTED",
            "MAINTENANCE_TICKET",
            ticketId,
            notes
        );

        return ticket;
    }

    @Transactional
    public MaintenanceTicket approveRepair(
            Long ticketId, String notes, String username) {

        MaintenanceTicket ticket = getTicketForUpdate(ticketId);

        if (ticket.getStatus() != TicketStatus.AWAITING_APPROVAL) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Ticket is not awaiting approval"
            );
        }

        if (notes == null || notes.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Approval notes are required"
            );
        }

        ticket.setApprovalNotes(notes);
        ticket.setStatus(TicketStatus.CLOSED);

        auditService.record(
            username,
            "REPAIR_APPROVED_AND_CLOSED",
            "MAINTENANCE_TICKET",
            ticketId,
            notes
        );

        return ticket;
    }

    @Transactional
    public MaintenanceTicket rejectRepair(
            Long ticketId, String notes, String username) {

        MaintenanceTicket ticket = getTicketForUpdate(ticketId);

        if (ticket.getStatus() != TicketStatus.AWAITING_APPROVAL) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Ticket is not awaiting approval"
            );
        }

        if (notes == null || notes.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Rejection reason is required"
            );
        }

        ticket.setApprovalNotes(notes);
        ticket.setStatus(TicketStatus.REJECTED);

        auditService.record(
            username,
            "REPAIR_REJECTED",
            "MAINTENANCE_TICKET",
            ticketId,
            notes
        );

        return ticket;
    }

    @Transactional
    public MaintenanceTicket triageTicket(
            Long ticketId, String username) {

        MaintenanceTicket ticket = getTicketForUpdate(ticketId);

        if (ticket.getStatus() != TicketStatus.OPEN) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Only open tickets can be triaged"
            );
        }

        ticket.setStatus(TicketStatus.TRIAGED);

        auditService.record(
            username,
            "TICKET_TRIAGED",
            "MAINTENANCE_TICKET",
            ticketId,
            ticket.getPriority() + ": " + ticket.getTriageReason()
        );

        return ticket;
    }

    @Transactional(readOnly = true)
    public List<MaintenanceTicket> getAllTickets() {
        return ticketRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<MaintenanceTicket> getMyAssignedTickets(String username) {
        AppUser user = getUser(username);
        return ticketRepository.findByAssignedTechnicianId(user.getId());
    }

    private AppUser getUser(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "User not found"
            ));
    }

    private MaintenanceTicket getTicketForUpdate(Long id) {
        return ticketRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Ticket not found"
            ));
    }

    private void requireAssignedTechnician(
            MaintenanceTicket ticket, String username) {

        if (ticket.getAssignedTechnician() == null ||
            !ticket.getAssignedTechnician().getUsername().equals(username)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Only the assigned technician can perform this action"
            );
        }
    }
}
