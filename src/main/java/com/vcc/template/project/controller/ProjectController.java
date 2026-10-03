package com.vcc.template.project.controller;

import com.vcc.template.common.dto.PaginationResponse;
import com.vcc.template.project.dto.*;
import com.vcc.template.project.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Controller for project management operations.
 * Authorization is handled by Spring Security PreAuthorize annotations.
 * Provides CRUD operations for managing tenant projects with schema isolation.
 */
@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Project Management", description = "Project management operations for multi-tenant administration")
class ProjectController {

        private final ProjectService projectService;

        @PostMapping(version = "1.0")
        @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
        @Operation(summary = "Create a new project", description = "Creates a new project with isolated database schema. "
                        +
                        "The project code must be unique and will be used as the schema name. " +
                        "Optionally accepts an icon path from a pre-uploaded file.")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "Project created successfully", content = @Content(schema = @Schema(implementation = ProjectResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid request or validation error"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden - requires SYSTEM_ADMIN role"),
                        @ApiResponse(responseCode = "409", description = "Project code already exists"),
                        @ApiResponse(responseCode = "500", description = "Schema creation or migration failed")
        })
        ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody CreateProjectRequest request) {
                log.info("Creating project with code: {}", request.projectCode());

                ProjectResponse response = projectService.createProject(request);

                log.info("Project created successfully: {}", response.getProjectCode());
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        @GetMapping(version = "1.0")
        @Operation(summary = "List projects with pagination", description = "Retrieves a paginated list of projects. " +
                        "Supports optional filtering by status and pagination parameters.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Projects retrieved successfully", content = @Content(schema = @Schema(implementation = PaginationResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden - requires SYSTEM_ADMIN role")
        })
        ResponseEntity<PaginationResponse<ProjectListResponse>> listProjects(
                        @Valid @ModelAttribute ProjectSearchRequest request) {
                PaginationResponse<ProjectListResponse> response = projectService.listProjects(request);
                return ResponseEntity.ok(response);
        }

        @GetMapping(path = "/{projectCode}", version = "1.0")
        @PreAuthorize("@projectSecurity.hasAnyRole(#projectCode, 'PROJECT_ADMIN', 'MAINTAINER', 'DEVELOPER')")
        @Operation(summary = "Get project details", description = "Retrieves detailed information about a specific project by its project code.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Project retrieved successfully", content = @Content(schema = @Schema(implementation = ProjectResponse.class))),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden - requires SYSTEM_ADMIN role"),
                        @ApiResponse(responseCode = "404", description = "Project not found")
        })
        ResponseEntity<ProjectResponse> getProject(@PathVariable String projectCode) {
                log.info("Retrieving project: {}", projectCode);

                ProjectResponse response = projectService.getProject(projectCode);

                log.info("Project retrieved successfully: {}", projectCode);
                return ResponseEntity.ok(response);
        }

        @PutMapping(path = "/{projectCode}", version = "1.0")
        @PreAuthorize("@projectSecurity.hasRole(#projectCode, 'PROJECT_ADMIN')")
        @Operation(summary = "Update project", description = "Updates an existing project's mutable fields (name, status, icon). "
                        +
                        "Note: Project code is immutable and cannot be changed.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Project updated successfully", content = @Content(schema = @Schema(implementation = ProjectResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid request or validation error"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden - requires SYSTEM_ADMIN role"),
                        @ApiResponse(responseCode = "404", description = "Project not found")
        })
        ResponseEntity<ProjectResponse> updateProject(
                        @PathVariable String projectCode,
                        @Valid @RequestBody UpdateProjectRequest request) {
                log.info("Updating project: {}", projectCode);

                ProjectResponse response = projectService.updateProject(projectCode, request);

                log.info("Project updated successfully: {}", projectCode);
                return ResponseEntity.ok(response);
        }

        @DeleteMapping(path = "/{projectCode}", version = "1.0")
        @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
        @Operation(summary = "Delete project (soft delete)", description = "Soft deletes a project by setting its status to INACTIVE. "
                        +
                        "The project record and database schema are preserved for audit purposes.")
        @ApiResponses({
                        @ApiResponse(responseCode = "204", description = "Project deleted successfully"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden - requires SYSTEM_ADMIN role"),
                        @ApiResponse(responseCode = "404", description = "Project not found")
        })
        ResponseEntity<Void> deleteProject(@PathVariable String projectCode) {
                log.info("Deleting project (soft): {}", projectCode);
                projectService.deleteProject(projectCode);
                log.info("Project soft deleted: {}", projectCode);
                return ResponseEntity.noContent().build();
        }

        @DeleteMapping(path = "/{projectCode}/hard", version = "1.0")
        @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
        @Operation(summary = "Hard delete project (permanent)", description = "⚠️ PERMANENT: Drops the project's database schema (CASCADE) and removes the project "
                        +
                        "record entirely from the system. This cannot be undone. " +
                        "All tenant data, members, and configurations will be lost permanently.")
        @ApiResponses({
                        @ApiResponse(responseCode = "204", description = "Project permanently deleted"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden - requires SYSTEM_ADMIN role"),
                        @ApiResponse(responseCode = "404", description = "Project not found"),
                        @ApiResponse(responseCode = "500", description = "Schema drop failed")
        })
        ResponseEntity<Void> hardDeleteProject(@PathVariable String projectCode) {
                log.warn("Hard delete requested for project: {}", projectCode);
                projectService.hardDeleteProject(projectCode);
                return ResponseEntity.noContent().build();
        }

        @PatchMapping(path = "/{projectCode}/reactivate", version = "1.0")
        @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
        @Operation(summary = "Reactivate a project", description = "Sets an INACTIVE project back to ACTIVE status.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Project reactivated"),
                        @ApiResponse(responseCode = "404", description = "Project not found")
        })
        ResponseEntity<ProjectResponse> reactivateProject(@PathVariable String projectCode) {
                log.info("Reactivating project: {}", projectCode);
                ProjectResponse response = projectService.reactivateProject(projectCode);
                return ResponseEntity.ok(response);
        }
}
