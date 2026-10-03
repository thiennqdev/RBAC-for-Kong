package com.vcc.template.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new project.
 * Contains validation rules for project code, name, and optional icon path.
 */
public record CreateProjectRequest(
    @NotBlank(message = "{validation.projectCode.notBlank}")
    @Pattern(regexp = "^[a-zA-Z0-9-]{3,50}$", message = "{validation.projectCode.pattern}")
    String projectCode,
    
    @NotBlank(message = "{validation.projectName.notBlank}")
    @Size(max = 200, message = "{validation.projectName.size}")
    String projectName,
    
    @Size(max = 500, message = "{validation.iconPath.size}")
    String iconPath,  // Optional, pre-uploaded file path from /files/upload
    
    String idpType,
    String idpIssuerUrl,
    String idpJwksUrl,
    String idpClientId,
    String idpClientSecret,
    Boolean jwtAuthEnabled
) {}
