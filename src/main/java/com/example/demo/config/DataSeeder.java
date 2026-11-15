package com.example.demo.config;

import com.example.demo.user.AppUser;
import com.example.demo.user.AppUserRepository;
import com.example.demo.user.Role;
import com.example.demo.facility.Facility;
import com.example.demo.facility.FacilityRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedData(
            AppUserRepository userRepository,
            FacilityRepository facilityRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {

            String demoPassword = "CampusOps@123";

            createUser(userRepository, passwordEncoder,
                    "student1", "student1@campusops.local",
                    Role.STUDENT, demoPassword);

            createUser(userRepository, passwordEncoder,
                    "manager1", "manager1@campusops.local",
                    Role.FACILITY_MANAGER, demoPassword);

            createUser(userRepository, passwordEncoder,
                    "tech1", "tech1@campusops.local",
                    Role.TECHNICIAN, demoPassword);

            createUser(userRepository, passwordEncoder,
                    "approver1", "approver1@campusops.local",
                    Role.APPROVER, demoPassword);

            createUser(userRepository, passwordEncoder,
                    "admin1", "admin1@campusops.local",
                    Role.ADMIN, demoPassword);

            createFacility(facilityRepository,
                    "Computer Lab A", "LAB",
                    "Block A - First Floor", 40, true);

            createFacility(facilityRepository,
                    "Seminar Hall", "HALL",
                    "Main Block - Ground Floor", 150, true);

            createFacility(facilityRepository,
                    "Library Block", "LIBRARY",
                    "Library Building", 100, true);

        };
    }

    private void createUser(
            AppUserRepository repository,
            PasswordEncoder encoder,
            String username,
            String email,
            Role role,
            String password) {

        if (!repository.existsByUsername(username)) {
            AppUser user = new AppUser();
            user.setUsername(username);
            user.setEmail(email);
            user.setPassword(encoder.encode(password));
            user.setRole(role);
            user.setActive(true);
            repository.save(user);
        }
    }

    private void createFacility(
            FacilityRepository repository,
            String name,
            String type,
            String location,
            int capacity,
            boolean accessible) {

        boolean exists = repository.findByActiveTrue()
                .stream()
                .anyMatch(f -> f.getName().equals(name));

        if (!exists) {
            Facility facility = new Facility();
            facility.setName(name);
            facility.setFacilityType(type);
            facility.setLocation(location);
            facility.setCapacity(capacity);
            facility.setAccessible(accessible);
            facility.setActive(true);
            repository.save(facility);
        }
    }
}
