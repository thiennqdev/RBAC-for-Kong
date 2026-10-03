package com.vcc.template.project.dto;

import com.vcc.template.project.entity.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating an existing project.
 * Contains validation rules for mutable fields: project name, status, icon path, and IdP configuration.
 * Note: projectCode is immutable and cannot be updated.
 */
public record UpdateProjectRequest(
    @NotBlank(message = "{validation.projectName.notBlank}")
    @Size(max = 200, message = "{validation.projectName.size}")
    String projectName,
    
    @NotNull(message = "{validation.status.notNull}")
    ProjectStatus status,
    
    @Size(max = 500, message = "{validation.iconPath.size}")
    String iconPath,  // Optional, new file path from /v1/files/upload

    @Size(max = 30)
    String idpType,  // DEFAULT, CUSTOM_KEYCLOAK, AUTH0, CUSTOM_OIDC

    @Size(max = 500)
    String idpIssuerUrl,  // Custom IdP issuer URL

    @Size(max = 500)
    String idpJwksUrl,  // Custom IdP JWKS URL

    @Size(max = 200)
    String idpClientId, // Custom OIDC Client ID

    @Size(max = 500)
    String idpClientSecret, // Custom OIDC Client Secret

    Boolean jwtAuthEnabled
) {}
