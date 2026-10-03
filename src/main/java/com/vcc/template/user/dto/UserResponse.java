package com.vcc.template.user.dto;

import com.vcc.template.user.entity.UserStatus;

import java.time.LocalDateTime;

/**
 * Response DTO for user data.
 */
public record UserResponse(
        Long id,
        String keycloakId,
        String username,
        String email,
        String firstName,
        String lastName,
        UserStatus status,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy) {
}
