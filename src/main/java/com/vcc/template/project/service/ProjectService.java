package com.vcc.template.project.service;

import com.vcc.template.common.dto.PaginationResponse;
import com.vcc.template.project.dto.*;

/**
 * Service interface for project management operations.
 * Handles business logic for creating, retrieving, updating, and deleting
 * projects.
 * All operations are restricted to users with SYSTEM_ADMIN role.
 */
public interface ProjectService {

    /**
     * Create a new project with isolated database schema.
     * 
     * @param request the project creation request
     * @return ProjectResponse containing the created project details
     * @throws com.vcc.template.common.exception.BusinessException if project code
     *                                                             already exists,
     *                                                             icon path is
     *                                                             invalid, or
     *                                                             schema creation
     *                                                             fails
     */
    ProjectResponse createProject(CreateProjectRequest request);

    /**
     * Retrieve a project by its unique project code.
     * 
     * @param projectCode the project code to search for
     * @return ProjectResponse containing the project details
     * @throws com.vcc.template.common.exception.BusinessException if project not
     *                                                             found
     */
    ProjectResponse getProject(String projectCode);

    /**
     * List projects with pagination and optional status filtering.
     * Uses Specification pattern for dynamic query building.
     * 
     * @param request the search request with pagination and filter parameters
     * @return PaginationResponse containing the list of projects and metadata
     */
    PaginationResponse<ProjectListResponse> listProjects(ProjectSearchRequest request);

    /**
     * Check if a project exists by its project code.
     * 
     * @param projectCode the project code to check
     * @return true if project exists, false otherwise
     */
    boolean existsByProjectCode(String projectCode);

    /**
     * Update an existing project's mutable fields.
     * Note: projectCode is immutable and cannot be changed.
     * 
     * @param projectCode the project code to update
     * @param request     the update request with new values
     * @return ProjectResponse containing the updated project details
     * @throws com.vcc.template.common.exception.BusinessException if project not
     *                                                             found
     *                                                             or icon path is
     *                                                             invalid
     */
    ProjectResponse updateProject(String projectCode, UpdateProjectRequest request);

    /**
     * Soft delete a project by setting its status to INACTIVE.
     * The project record and database schema are preserved.
     * 
     * @param projectCode the project code to delete
     * @throws com.vcc.template.common.exception.BusinessException if project not
     *                                                             found
     */
    void deleteProject(String projectCode);

    /**
     * Permanently deletes a project and all its data.
     * ⚠️ This drops the project's database schema (CASCADE), removes all members,
     * and deletes the project row from the database. This action is irreversible.
     *
     * @param projectCode the unique project code
     */
    void hardDeleteProject(String projectCode);

    /**
     * Reactivates an INACTIVE project, setting its status back to ACTIVE.
     * 
     * @param projectCode the unique project code
     */
    ProjectResponse reactivateProject(String projectCode);

}
