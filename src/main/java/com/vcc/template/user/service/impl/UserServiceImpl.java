package com.vcc.template.user.service.impl;

import com.vcc.template.common.dto.PaginationResponse;
import com.vcc.template.common.keycloak.KeycloakAdminService;
import com.vcc.template.common.util.ResponseUtil;
import com.vcc.template.user.dto.CreateUserRequest;
import com.vcc.template.user.dto.UpdateUserRequest;
import com.vcc.template.user.dto.UserResponse;
import com.vcc.template.user.dto.UserSearchRequest;
import com.vcc.template.user.entity.User;
import com.vcc.template.user.entity.UserStatus;
import com.vcc.template.user.errorcode.UserErrorCode;
import com.vcc.template.user.mapper.UserMapper;
import com.vcc.template.user.repository.UserRepository;
import com.vcc.template.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of UserService with Keycloak sync and compensating
 * transactions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
class UserServiceImpl implements UserService {

    private static final String DEFAULT_TEMPORARY_PASSWORD = "ChangeMe@123";

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final KeycloakAdminService keycloakAdminService;

    /**
     * Creates a user with compensating transaction:
     * 1. Validate uniqueness in local DB
     * 2. Create user in Keycloak → get keycloakId
     * 3. Save to local DB
     * 4. If DB save fails → delete from Keycloak (compensate)
     */
    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        log.debug("Creating user: {}", request.username());

        // 1. Validate uniqueness
        if (userRepository.existsByUsername(request.username())) {
            throw UserErrorCode.USERNAME_ALREADY_EXISTS.toException(request.username());
        }
        if (userRepository.existsByEmail(request.email())) {
            throw UserErrorCode.EMAIL_ALREADY_EXISTS.toException(request.email());
        }

        // 2. Create in Keycloak first (use default password if not provided)
        String effectivePassword = (request.password() != null && !request.password().isBlank())
                ? request.password()
                : DEFAULT_TEMPORARY_PASSWORD;

        String keycloakId = keycloakAdminService.createUser(
                request.username(), request.email(),
                request.firstName(), request.lastName(),
                effectivePassword);

        // 3. Save to local DB with compensating transaction
        try {
            User user = new User();
            user.setKeycloakId(keycloakId);
            user.setUsername(request.username());
            user.setEmail(request.email());
            user.setFirstName(request.firstName());
            user.setLastName(request.lastName());
            user.setStatus(UserStatus.ACTIVE);

            User saved = userRepository.save(user);
            log.info("User created successfully: {} (Keycloak ID: {})", saved.getUsername(), keycloakId);
            return userMapper.toResponse(saved);
        } catch (Exception e) {
            // 4. Compensating transaction: delete from Keycloak if DB save fails
            log.error("Failed to save user to DB, rolling back Keycloak user: {}", keycloakId, e);
            keycloakAdminService.deleteUser(keycloakId);
            throw UserErrorCode.USER_CREATION_FAILED.toException(e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> UserErrorCode.USER_NOT_FOUND.toException(userId.toString()));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<UserResponse> listUsers(UserSearchRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        Page<User> page = userRepository.search(request.getKeyword(), request.getStatus(), pageable);
        Page<UserResponse> responsePage = page.map(userMapper::toResponse);
        return ResponseUtil.toPagingResponse(responsePage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> searchUsers(String keyword) {
        Pageable pageable = PageRequest.of(0, 20);
        Page<User> page = userRepository.search(keyword, UserStatus.ACTIVE, pageable);
        return page.getContent().stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public UserResponse syncUserToKeycloak(Long userId) {
        log.debug("Syncing user {} to Keycloak", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> UserErrorCode.USER_NOT_FOUND.toException(userId.toString()));

        if (user.getKeycloakId() != null && !user.getKeycloakId().isBlank()) {
            log.info("User {} already has Keycloak ID: {}, skipping sync", user.getUsername(), user.getKeycloakId());
            return userMapper.toResponse(user);
        }

        // Create in Keycloak with default temporary password
        String keycloakId = keycloakAdminService.createUser(
                user.getUsername(), user.getEmail(),
                user.getFirstName(), user.getLastName(),
                DEFAULT_TEMPORARY_PASSWORD);

        user.setKeycloakId(keycloakId);
        User saved = userRepository.save(user);
        log.info("User {} synced to Keycloak with ID: {}", user.getUsername(), keycloakId);
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public long syncAllUnsyncedUsersToKeycloak() {
        log.info("Starting batch sync of all unsynced users to Keycloak");
        List<User> unsyncedUsers = userRepository.findUnsyncedUsers();

        if (unsyncedUsers.isEmpty()) {
            log.info("No unsynced users found.");
            return 0;
        }

        long successCount = 0;
        for (User user : unsyncedUsers) {
            try {
                log.debug("Batch syncing user {} to Keycloak", user.getUsername());
                String keycloakId = keycloakAdminService.createUser(
                        user.getUsername(), user.getEmail(),
                        user.getFirstName(), user.getLastName(),
                        DEFAULT_TEMPORARY_PASSWORD);

                user.setKeycloakId(keycloakId);
                // Save immediately inside loop to persist the updated keycloak_id
                userRepository.saveAndFlush(user);
                successCount++;
                log.info("User {} synced to Keycloak successfully in batch process with ID: {}", user.getUsername(),
                        keycloakId);
            } catch (Exception e) {
                log.error("Failed to sync user {} to Keycloak during batch process", user.getUsername(), e);
            }
        }

        log.info("Finished batch sync. Successfully synced {} out of {} users", successCount, unsyncedUsers.size());
        return successCount;
    }

    /**
     * Updates a user with Keycloak sync.
     * Updates Keycloak first, then local DB. If DB fails, Keycloak state is
     * inconsistent
     * but manageable since the user can retry.
     */
    @Override
    @Transactional
    public UserResponse updateUser(Long userId, UpdateUserRequest request) {
        log.debug("Updating user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> UserErrorCode.USER_NOT_FOUND.toException(userId.toString()));

        // Check email uniqueness if changing
        if (request.email() != null && !request.email().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.email())) {
                throw UserErrorCode.EMAIL_ALREADY_EXISTS.toException(request.email());
            }
        }

        // Update Keycloak first
        keycloakAdminService.updateUser(user.getKeycloakId(),
                request.email() != null ? request.email() : user.getEmail(),
                request.firstName() != null ? request.firstName() : user.getFirstName(),
                request.lastName() != null ? request.lastName() : user.getLastName());

        // Update local DB
        if (request.email() != null)
            user.setEmail(request.email());
        if (request.firstName() != null)
            user.setFirstName(request.firstName());
        if (request.lastName() != null)
            user.setLastName(request.lastName());

        User saved = userRepository.save(user);
        log.info("User updated successfully: {}", saved.getUsername());
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void lockUser(Long userId) {
        log.debug("Locking user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> UserErrorCode.USER_NOT_FOUND.toException(userId.toString()));

        if (user.getStatus() == UserStatus.LOCKED) {
            throw UserErrorCode.USER_ALREADY_LOCKED.toException(user.getUsername());
        }

        // Disable in Keycloak
        keycloakAdminService.setUserEnabled(user.getKeycloakId(), false);

        // Update local
        user.setStatus(UserStatus.LOCKED);
        userRepository.save(user);
        log.info("User locked: {}", user.getUsername());
    }

    @Override
    @Transactional
    public void unlockUser(Long userId) {
        log.debug("Unlocking user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> UserErrorCode.USER_NOT_FOUND.toException(userId.toString()));

        if (user.getStatus() == UserStatus.ACTIVE) {
            throw UserErrorCode.USER_ALREADY_ACTIVE.toException(user.getUsername());
        }

        // Enable in Keycloak
        keycloakAdminService.setUserEnabled(user.getKeycloakId(), true);

        // Update local
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        log.info("User unlocked: {}", user.getUsername());
    }
}
