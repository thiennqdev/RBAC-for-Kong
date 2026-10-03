package com.vcc.template.access.controller;

import com.vcc.template.access.dto.AssignMemberRequest;
import com.vcc.template.access.dto.BatchAssignMemberRequest;
import com.vcc.template.access.dto.ProjectMemberResponse;
import com.vcc.template.access.dto.UpdateMemberRoleRequest;
import com.vcc.template.access.service.ProjectMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

/**
 * Controller for managing project members and their roles.
 * Authorization is handled by Spring Security PreAuthorize annotations.
 */
@RestController
@RequestMapping("/projects/{projectCode}/members")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Project Members", description = "Manage user-project-role assignments")
class ProjectMemberController {

    private final ProjectMemberService projectMemberService;

    @PostMapping(version = "1.0")
    @PreAuthorize("@projectSecurity.hasRole(#projectCode, 'PROJECT_ADMIN')")
    @Operation(summary = "Assign user to project with role")
    ResponseEntity<ProjectMemberResponse> assignMember(
            @PathVariable String projectCode,
            @Valid @RequestBody AssignMemberRequest request) {
        log.info("Assigning user {} to project {} with role {}", request.userId(), projectCode, request.roleId());
        ProjectMemberResponse response = projectMemberService.assignMember(projectCode, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(path = "/batch", version = "1.0")
    @PreAuthorize("@projectSecurity.hasRole(#projectCode, 'PROJECT_ADMIN')")
    @Operation(summary = "Batch assign multiple users to project with same role", description = "Assigns multiple users at once. Already-assigned users are skipped (not errors).")
    ResponseEntity<List<ProjectMemberResponse>> batchAssignMembers(
            @PathVariable String projectCode,
            @Valid @RequestBody BatchAssignMemberRequest request) {
        log.info("Batch assigning {} users to project {} with role {}",
                request.userIds().size(), projectCode, request.roleId());
        List<ProjectMemberResponse> responses = projectMemberService.batchAssignMembers(projectCode, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    @GetMapping(version = "1.0")
    @PreAuthorize("@projectSecurity.hasAnyRole(#projectCode, 'PROJECT_ADMIN', 'MAINTAINER', 'DEVELOPER')")
    @Operation(summary = "List project members with roles")
    ResponseEntity<List<ProjectMemberResponse>> listMembers(@PathVariable String projectCode) {
        List<ProjectMemberResponse> members = projectMemberService.listMembers(projectCode);
        return ResponseEntity.ok(members);
    }

    @PutMapping(path = "/{userId}", version = "1.0")
    @PreAuthorize("@projectSecurity.hasRole(#projectCode, 'PROJECT_ADMIN')")
    @Operation(summary = "Update member's role in project")
    ResponseEntity<ProjectMemberResponse> updateMemberRole(
            @PathVariable String projectCode,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateMemberRoleRequest request) {
        log.info("Updating role for user {} in project {}", userId, projectCode);
        ProjectMemberResponse response = projectMemberService.updateMemberRole(projectCode, userId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping(path = "/{userId}", version = "1.0")
    @PreAuthorize("@projectSecurity.hasRole(#projectCode, 'PROJECT_ADMIN')")
    @Operation(summary = "Remove user from project")
    ResponseEntity<Void> removeMember(@PathVariable String projectCode, @PathVariable Long userId) {
        log.info("Removing user {} from project {}", userId, projectCode);
        projectMemberService.removeMember(projectCode, userId);
        return ResponseEntity.noContent().build();
    }
}
