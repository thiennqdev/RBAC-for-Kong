package com.vcc.template.common.keycloak;

import com.vcc.template.user.entity.User;
import com.vcc.template.user.entity.UserStatus;
import com.vcc.template.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserSyncService {

    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean syncUser(UserRepresentation kcUser) {
        // Check if user exists by Keycloak ID
        var existingUserByKeycloakId = userRepository.findByKeycloakId(kcUser.getId());
        if (existingUserByKeycloakId.isPresent()) {
            return false; // Already exists and synced by ID
        }

        // Check if user exists by Username (to avoid duplicate key error)
        var existingUserByUsername = userRepository.findByUsername(kcUser.getUsername());
        User user = null;
        if (existingUserByUsername.isPresent()) {
            user = existingUserByUsername.get();
            log.info("Updating existing user (found by username) with new Keycloak ID: {}", kcUser.getUsername());
        } else {
            // Check if user exists by Email (to avoid duplicate key error)
            String email = kcUser.getEmail() != null ? kcUser.getEmail() : kcUser.getUsername() + "@local";
            var existingUserByEmail = userRepository.findByEmailIgnoreCase(email);
            if (existingUserByEmail.isPresent()) {
                user = existingUserByEmail.get();
                log.info("Updating existing user (found by email) with new Keycloak ID: {}", kcUser.getUsername());
            } else {
                user = new User();
                user.setCreatedBy("system-bootstrap");
            }
        }

        user.setKeycloakId(kcUser.getId());
        user.setUsername(kcUser.getUsername());
        user.setEmail(kcUser.getEmail() != null ? kcUser.getEmail() : kcUser.getUsername() + "@local");
        user.setFirstName(kcUser.getFirstName());
        user.setLastName(kcUser.getLastName());
        user.setStatus(kcUser.isEnabled() ? UserStatus.ACTIVE : UserStatus.LOCKED);
        user.setUpdatedBy("system-bootstrap");

        userRepository.save(user);
        log.info("Synced admin user from Keycloak: {} (ID: {})", kcUser.getUsername(), kcUser.getId());
        return true;
    }
}
