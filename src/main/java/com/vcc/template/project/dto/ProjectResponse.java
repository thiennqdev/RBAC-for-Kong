package com.vcc.template.project.dto;

import com.vcc.template.project.entity.ProjectStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Response DTO for project data.
 * Contains all project information including audit fields and icon URL.
 * Uses class (not record) to ensure compatibility with Redis cache
 * serialization
 * when using Jackson's DefaultTyping.NON_FINAL.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {
    private String projectCode;
    private String projectName;
    private String iconPath;
    private ProjectStatus status;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
    private String idpType;
    private String idpIssuerUrl;
    private String idpJwksUrl;
    private String idpClientId;
    private String idpClientSecret;
    private Boolean jwtAuthEnabled;
}
