package com.vcc.template.project.dto;

import com.vcc.template.common.dto.BaseSearchRequest;
import com.vcc.template.project.entity.ProjectStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for searching and listing projects with pagination.
 * Extends BaseSearchRequest to include pagination and sorting capabilities.
 * Supports multiple optional filters for flexible project searching.
 */
@Getter
@Setter
public class ProjectSearchRequest extends BaseSearchRequest {
    
    /**
     * Optional status filter.
     * When provided, only projects with matching status are returned.
     */
    private ProjectStatus status;
    
    /**
     * Optional project name filter (partial match, case-insensitive).
     * When provided, searches for projects whose name contains this value.
     */
    private String projectName;
    
    /**
     * Optional project code filter (partial match, case-insensitive).
     * When provided, searches for projects whose code contains this value.
     */
    private String projectCode;
}
