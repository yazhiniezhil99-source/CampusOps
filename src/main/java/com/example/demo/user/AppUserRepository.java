
package com.example.demo.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    Optional<AppUser> findByEmail(String email);

    List<AppUser> findByRoleAndActiveTrue(Role role);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
