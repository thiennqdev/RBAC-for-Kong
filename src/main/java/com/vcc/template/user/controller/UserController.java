package com.vcc.template.user.controller;

import com.vcc.template.common.dto.PaginationResponse;
import com.vcc.template.user.dto.CreateUserRequest;
import com.vcc.template.user.dto.UpdateUserRequest;
import com.vcc.template.user.dto.UserResponse;
import com.vcc.template.user.dto.UserSearchRequest;
import com.vcc.template.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller for user management operations.
 * All endpoints require SYSTEM_ADMIN role.
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management", description = "User administration with Keycloak sync")
class UserController {

    private final UserService userService;

    @PostMapping(version = "1.0")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Create a new user", description = "Creates user in both Keycloak and local DB with compensating transaction")
    ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("Creating user: {}", request.username());
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping(version = "1.0")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Search users", description = "Search users with keyword and status filter, paginated")
    ResponseEntity<PaginationResponse<UserResponse>> listUsers(@Valid @ModelAttribute UserSearchRequest request) {
        PaginationResponse<UserResponse> response = userService.listUsers(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping(path = "/search", version = "1.0")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Search users for autocomplete", description = "Lightweight search for assign-member multi-select. Returns max 20 active users.")
    ResponseEntity<List<UserResponse>> searchUsers(
            @RequestParam(required = false, defaultValue = "") String keyword) {
        List<UserResponse> users = userService.searchUsers(keyword);
        return ResponseEntity.ok(users);
    }

    @PostMapping(path = "/{userId}/sync", version = "1.0")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Sync user to Keycloak", description = "Syncs a local DB user to Keycloak with a temporary password. User will be forced to change password on first login.")
    ResponseEntity<UserResponse> syncUserToKeycloak(@PathVariable Long userId) {
        log.info("Syncing user {} to Keycloak", userId);
        UserResponse response = userService.syncUserToKeycloak(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping(path = "/sync-all", version = "1.0")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Batch sync all unsynced users to Keycloak", description = "Finds all users in local DB without a Keycloak ID and syncs them in batch. Returns the number of successfully synced users.")
    ResponseEntity<Long> syncAllUnsyncedUsersToKeycloak() {
        log.info("Batch syncing all unsynced users to Keycloak");
        long syncedCount = userService.syncAllUnsyncedUsersToKeycloak();
        return ResponseEntity.ok(syncedCount);
    }

    @GetMapping(path = "/{userId}", version = "1.0")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Get user details")
    ResponseEntity<UserResponse> getUser(@PathVariable Long userId) {
        UserResponse response = userService.getUser(userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping(path = "/{userId}", version = "1.0")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Update user", description = "Updates user in both Keycloak and local DB")
    ResponseEntity<UserResponse> updateUser(@PathVariable Long userId, @Valid @RequestBody UpdateUserRequest request) {
        log.info("Updating user: {}", userId);
        UserResponse response = userService.updateUser(userId, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping(path = "/{userId}/lock", version = "1.0")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Lock user", description = "Locks user account and disables in Keycloak")
    ResponseEntity<Void> lockUser(@PathVariable Long userId) {
        log.info("Locking user: {}", userId);
        userService.lockUser(userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(path = "/{userId}/unlock", version = "1.0")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Unlock user", description = "Unlocks user account and enables in Keycloak")
    ResponseEntity<Void> unlockUser(@PathVariable Long userId) {
        log.info("Unlocking user: {}", userId);
        userService.unlockUser(userId);
        return ResponseEntity.noContent().build();
    }
}
