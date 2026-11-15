package com.example.demo.booking;

import com.example.demo.audit.AuditService;
import com.example.demo.facility.Facility;
import com.example.demo.facility.FacilityRepository;
import com.example.demo.user.AppUser;
import com.example.demo.user.AppUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final FacilityRepository facilityRepository;
    private final AppUserRepository userRepository;
    private final AuditService auditService;

    public BookingService(
            BookingRepository bookingRepository,
            FacilityRepository facilityRepository,
            AppUserRepository userRepository,
            AuditService auditService) {
        this.bookingRepository = bookingRepository;
        this.facilityRepository = facilityRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional
    public Booking createBooking(BookingRequest request, String username) {

        if (request.getStartTime() == null ||
            request.getEndTime() == null ||
            !request.getEndTime().isAfter(request.getStartTime()) ||
            !request.getStartTime().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Invalid booking time range");
        }

        if (request.getRequestKey() == null ||
            request.getRequestKey().isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Request key is required");
        }

        AppUser user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "User not found"));

        var existing = bookingRepository.findByRequestKey(
            request.getRequestKey());

        if (existing.isPresent()) {
            Booking previous = existing.get();

            if (!previous.getRequestedBy().getId().equals(user.getId())) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Request key already belongs to another user");
            }

            return previous;
        }

        Facility facility = facilityRepository.findByIdForUpdate(
            request.getFacilityId())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Facility not found"));

        if (!facility.isActive()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Facility is not available");
        }

        boolean overlap = bookingRepository.hasOverlappingBooking(
            facility.getId(),
            request.getStartTime(),
            request.getEndTime(),
            List.of(BookingStatus.PENDING, BookingStatus.APPROVED));

        if (overlap) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Facility is already booked for this time");
        }

        Booking booking = new Booking();
        booking.setRequestKey(request.getRequestKey());
        booking.setFacility(facility);
        booking.setRequestedBy(user);
        booking.setStartTime(request.getStartTime());
        booking.setEndTime(request.getEndTime());
        booking.setPurpose(request.getPurpose());
        booking.setStatus(BookingStatus.PENDING);

        Booking saved = bookingRepository.saveAndFlush(booking);

        auditService.record(username, "BOOKING_CREATED",
            "BOOKING", saved.getId(), "Facility: " + facility.getName());

        return saved;
    }

    @Transactional
    public Booking approveBooking(Long id, String username) {
        return changeStatus(id, username, BookingStatus.PENDING,
            BookingStatus.APPROVED, "BOOKING_APPROVED");
    }

    @Transactional
    public Booking rejectBooking(Long id, String username) {
        return changeStatus(id, username, BookingStatus.PENDING,
            BookingStatus.REJECTED, "BOOKING_REJECTED");
    }

    @Transactional
    public Booking completeBooking(Long id, String username) {
        return changeStatus(id, username, BookingStatus.APPROVED,
            BookingStatus.COMPLETED, "BOOKING_COMPLETED");
    }

    @Transactional
    public Booking cancelBooking(Long id, String username) {
        Booking booking = findBooking(id);

        if (!booking.getRequestedBy().getUsername().equals(username)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "You can only cancel your own booking");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Only pending bookings can be cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        Booking saved = bookingRepository.saveAndFlush(booking);

        auditService.record(username, "BOOKING_CANCELLED",
            "BOOKING", saved.getId(), "Booking cancelled by requester");

        return saved;
    }

    private Booking changeStatus(
            Long id, String username,
            BookingStatus expected, BookingStatus target,
            String action) {

        Booking booking = findBooking(id);

        if (booking.getStatus() != expected) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Invalid booking transition from " + booking.getStatus());
        }

        booking.setStatus(target);
        Booking saved = bookingRepository.saveAndFlush(booking);

        auditService.record(username, action, "BOOKING",
            saved.getId(), "Status changed to " + target);

        return saved;
    }

    private Booking findBooking(Long id) {
        return bookingRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Booking not found"));
    }

    @Transactional(readOnly = true)
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Booking> getMyBookings(String username) {
        AppUser user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "User not found"));

        return bookingRepository.findByRequestedById(user.getId());
    }
}
