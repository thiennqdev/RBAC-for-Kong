package com.vcc.template.access.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request DTO for batch-assigning multiple users to a project with a role.
 */
public record BatchAssignMemberRequest(
        @NotEmpty(message = "{validation.userIds.notEmpty}") List<@NotNull Long> userIds,

        @NotNull(message = "{validation.roleId.notNull}") String roleId) {
}
