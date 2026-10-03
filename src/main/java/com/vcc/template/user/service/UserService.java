package com.vcc.template.user.service;

import com.vcc.template.common.dto.PaginationResponse;
import com.vcc.template.user.dto.CreateUserRequest;
import com.vcc.template.user.dto.UpdateUserRequest;
import com.vcc.template.user.dto.UserResponse;
import com.vcc.template.user.dto.UserSearchRequest;

import java.util.List;

/**
 * Service interface for user management operations.
 */
public interface UserService {

    UserResponse createUser(CreateUserRequest request);

    UserResponse getUser(Long userId);

    PaginationResponse<UserResponse> listUsers(UserSearchRequest request);

    /**
     * Search users by keyword (username, email, name) for
     * autocomplete/multi-select.
     * Returns at most 20 active users matching the keyword.
     */
    List<UserResponse> searchUsers(String keyword);

    /**
     * Sync a local DB user (without keycloakId) to Keycloak.
     * Sets a default temporary password with UPDATE_PASSWORD required action.
     */
    UserResponse syncUserToKeycloak(Long userId);

    /**
     * Sync all local DB users without a keycloakId to Keycloak.
     * Sets a default temporary password with UPDATE_PASSWORD required action.
     * Returns the count of successfully synced users.
     */
    long syncAllUnsyncedUsersToKeycloak();

    UserResponse updateUser(Long userId, UpdateUserRequest request);

    void lockUser(Long userId);

    void unlockUser(Long userId);
}
