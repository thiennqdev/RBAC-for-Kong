package com.vcc.template.access.dto;

import java.time.LocalDateTime;

/**
 * Response DTO for a project member listing.
 */
public record ProjectMemberResponse(
        Long userId,
        String username,
        String email,
        String firstName,
        String lastName,
        String roleName,
        LocalDateTime assignedAt) {
}
