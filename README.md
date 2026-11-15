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