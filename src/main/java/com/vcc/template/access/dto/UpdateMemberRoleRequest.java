package com.vcc.template.access.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for updating a member's role in a project.
 */
public record UpdateMemberRoleRequest(
        @NotNull(message = "{validation.roleId.notNull}") String roleId) {
}
