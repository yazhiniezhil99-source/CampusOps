package com.example.demo.booking;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('STUDENT','FACILITY_MANAGER','ADMIN')")
    public Booking createBooking(
            @Valid @RequestBody BookingRequest request,
            Authentication authentication) {
        return bookingService.createBooking(request, authentication.getName());
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public List<Booking> getMyBookings(Authentication authentication) {
        return bookingService.getMyBookings(authentication.getName());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('FACILITY_MANAGER','ADMIN')")
    public List<Booking> getAllBookings() {
        return bookingService.getAllBookings();
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('FACILITY_MANAGER','ADMIN')")
    public Booking approveBooking(
            @PathVariable Long id, Authentication authentication) {
        return bookingService.approveBooking(id, authentication.getName());
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('FACILITY_MANAGER','ADMIN')")
    public Booking rejectBooking(
            @PathVariable Long id, Authentication authentication) {
        return bookingService.rejectBooking(id, authentication.getName());
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public Booking cancelBooking(
            @PathVariable Long id, Authentication authentication) {
        return bookingService.cancelBooking(id, authentication.getName());
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('FACILITY_MANAGER','ADMIN')")
    public Booking completeBooking(
            @PathVariable Long id, Authentication authentication) {
        return bookingService.completeBooking(id, authentication.getName());
    }
}
