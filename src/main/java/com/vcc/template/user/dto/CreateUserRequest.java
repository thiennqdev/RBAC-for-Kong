package com.vcc.template.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new user.
 * If no password is provided, a default temporary password will be set and the
 * user
 * will be required to change it on first login.
 */
public record CreateUserRequest(
                @NotBlank(message = "{validation.username.notBlank}") @Size(min = 3, max = 100, message = "{validation.username.size}") String username,

                @NotBlank(message = "{validation.email.notBlank}") @Email(message = "{validation.email.format}") String email,

                @Size(max = 100, message = "{validation.firstName.size}") String firstName,

                @Size(max = 100, message = "{validation.lastName.size}") String lastName,

                @Size(min = 6, max = 128, message = "{validation.password.size}") String password) {
}
