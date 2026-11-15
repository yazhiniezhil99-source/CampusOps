
package com.example.demo.facility;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/facilities")
public class FacilityController {

    private final FacilityRepository facilityRepository;

    public FacilityController(FacilityRepository facilityRepository) {
        this.facilityRepository = facilityRepository;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<Facility> getFacilities() {
        return facilityRepository.findByActiveTrue();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('FACILITY_MANAGER','ADMIN')")
    public Facility addFacility(@RequestBody Facility facility) {
        return facilityRepository.save(facility);
    }
}
