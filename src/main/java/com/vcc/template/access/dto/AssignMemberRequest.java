package com.vcc.template.access.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for assigning a user to a project with a role.
 */
public record AssignMemberRequest(
        @NotNull(message = "{validation.userId.notNull}") Long userId,

        @NotNull(message = "{validation.roleId.notNull}") String roleId) {
}
