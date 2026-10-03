package com.vcc.template.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating an existing user.
 */
public record UpdateUserRequest(
        @Email(message = "{validation.email.format}") String email,

        @Size(max = 100, message = "{validation.firstName.size}") String firstName,

        @Size(max = 100, message = "{validation.lastName.size}") String lastName) {
}
