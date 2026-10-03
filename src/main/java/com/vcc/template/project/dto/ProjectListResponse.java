package com.vcc.template.project.dto;

import com.vcc.template.project.entity.ProjectStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Class-based DTO projection for project list queries.
 * Used with @Query constructor expression to map query results directly to DTO.
 * This approach provides better performance and type safety.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectListResponse {
    
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
}
