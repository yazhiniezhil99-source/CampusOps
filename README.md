# CampusOps – Facility and Maintenance Management System

## Booking Management

The booking module allows users to view facilities and submit booking requests.

### Booking Validation

- Users must provide valid booking details.
- The selected facility must exist.
- Booking requests are checked for overlapping time slots.
- Conflicting bookings are rejected by the backend.
- Authorized managers can approve or reject booking requests.

### Backend Implementation

The booking functionality is implemented using Spring Boot REST APIs and MySQL persistence.

### Feature Branch

`feature/booking-validation`

This branch documents the booking validation workflow as a separate contribution.

## Maintenance and Audit Management

The maintenance module allows users to report facility issues and track their resolution.

### Maintenance Workflow

- Users can create maintenance tickets.
- Managers can review and assign tickets to technicians.
- Technicians can record inspection notes and submit repair details.
- Authorized approvers can approve completed repairs and close tickets.
- Important operations are recorded in the audit history.

### Audit Tracking

The audit module maintains records of important booking and maintenance activities, supporting accountability and traceability.

### Feature Branch

`feature/maintenance-audit`

This branch documents the maintenance processing and audit tracking workflow as a separate contribution.