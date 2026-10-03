package com.vcc.template.access.service.impl;

import com.vcc.template.access.dto.AssignMemberRequest;
import com.vcc.template.access.dto.BatchAssignMemberRequest;
import com.vcc.template.access.dto.ProjectMemberResponse;
import com.vcc.template.access.dto.UpdateMemberRoleRequest;
import com.vcc.template.access.entity.UserProjectRole;
import com.vcc.template.access.errorcode.AccessErrorCode;
import com.vcc.template.access.repository.UserProjectRoleRepository;
import com.vcc.template.access.service.ProjectMemberService;
import com.vcc.template.project.entity.Project;
import com.vcc.template.project.errorcode.ProjectErrorCode;
import com.vcc.template.project.repository.ProjectRepository;
import com.vcc.template.access.model.ProjectRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.vcc.template.user.entity.User;
import com.vcc.template.user.errorcode.UserErrorCode;
import com.vcc.template.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of ProjectMemberService for managing user-project-role
 * assignments.
 */
@Service
@RequiredArgsConstructor
@Slf4j
class ProjectMemberServiceImpl implements ProjectMemberService {

        private final UserProjectRoleRepository userProjectRoleRepository;
        private final ProjectRepository projectRepository;
        private final UserRepository userRepository;

        @Override
        @Transactional
        public ProjectMemberResponse assignMember(String projectCode, AssignMemberRequest request) {
                log.debug("Assigning user {} to project {} with role {}", request.userId(), projectCode,
                                request.roleId());

                Project project = findProjectByCode(projectCode);
                User user = userRepository.findById(request.userId())
                                .orElseThrow(() -> UserErrorCode.USER_NOT_FOUND
                                                .toException(request.userId().toString()));
                
                ProjectRole projectRole = parseRole(request.roleId().toString());

                if (userProjectRoleRepository.existsByUserIdAndProjectId(user.getId(), project.getId())) {
                        throw AccessErrorCode.MEMBER_ALREADY_ASSIGNED.toException(user.getUsername());
                }

                checkRoleAssignmentPrivilege(projectRole);

                UserProjectRole upr = new UserProjectRole();
                upr.setUser(user);
                upr.setProject(project);
                upr.setRoleName(projectRole);

                UserProjectRole saved = userProjectRoleRepository.save(upr);
                log.info("User {} assigned to project {} with role {}", user.getUsername(), projectCode,
                                projectRole.name());

                return toResponse(saved);
        }

        @Override
        @Transactional
        public List<ProjectMemberResponse> batchAssignMembers(String projectCode, BatchAssignMemberRequest request) {
                log.debug("Batch assigning {} users to project {} with role {}",
                                request.userIds().size(), projectCode, request.roleId());

                Project project = findProjectByCode(projectCode);
                ProjectRole projectRole = parseRole(request.roleId().toString());

                checkRoleAssignmentPrivilege(projectRole);

                List<ProjectMemberResponse> results = new ArrayList<>();
                List<String> skipped = new ArrayList<>();

                for (Long userId : request.userIds()) {
                        User user = userRepository.findById(userId)
                                        .orElseThrow(() -> UserErrorCode.USER_NOT_FOUND
                                                        .toException(userId.toString()));

                        // Skip already-assigned users instead of throwing
                        if (userProjectRoleRepository.existsByUserIdAndProjectId(user.getId(), project.getId())) {
                                skipped.add(user.getUsername());
                                continue;
                        }

                        UserProjectRole upr = new UserProjectRole();
                        upr.setUser(user);
                        upr.setProject(project);
                        upr.setRoleName(projectRole);

                        UserProjectRole saved = userProjectRoleRepository.save(upr);
                        results.add(toResponse(saved));
                }

                if (!skipped.isEmpty()) {
                        log.warn("Skipped already-assigned users: {}", skipped);
                }

                log.info("Batch assigned {} users to project {} with role {} ({} skipped)",
                                results.size(), projectCode, projectRole.name(), skipped.size());
                return results;
        }

        @Override
        @Transactional(readOnly = true)
        public List<ProjectMemberResponse> listMembers(String projectCode) {
                Project project = findProjectByCode(projectCode);
                return userProjectRoleRepository.findByProjectIdWithDetails(project.getId()).stream()
                                .map(this::toResponse)
                                .toList();
        }

        @Override
        @Transactional
        public ProjectMemberResponse updateMemberRole(String projectCode, Long userId,
                        UpdateMemberRoleRequest request) {
                log.debug("Updating role for user {} in project {}", userId, projectCode);

                Project project = findProjectByCode(projectCode);
                ProjectRole newRole = parseRole(request.roleId().toString());

                checkRoleAssignmentPrivilege(newRole);

                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getName() != null) {
                        userRepository.findByUsername(auth.getName()).ifPresent(currentUser -> {
                                if (currentUser.getId().equals(userId)) {
                                        throw AccessErrorCode.INVALID_ASSIGNMENT.toException("Cannot demote yourself");
                                }
                        });
                }

                UserProjectRole upr = userProjectRoleRepository.findByUserIdAndProjectId(userId, project.getId())
                                .orElseThrow(() -> AccessErrorCode.MEMBER_NOT_FOUND.toException(userId.toString()));

                upr.setRoleName(newRole);
                UserProjectRole saved = userProjectRoleRepository.save(upr);
                log.info("Updated role for user {} in project {} to {}", userId, projectCode, newRole.name());

                return toResponse(saved);
        }

        @Override
        @Transactional
        public void removeMember(String projectCode, Long userId) {
                log.debug("Removing user {} from project {}", userId, projectCode);

                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getName() != null) {
                        userRepository.findByUsername(auth.getName()).ifPresent(currentUser -> {
                                if (currentUser.getId().equals(userId)) {
                                        throw AccessErrorCode.CANNOT_REMOVE_YOURSELF.toException();
                                }
                        });
                }

                Project project = findProjectByCode(projectCode);
                UserProjectRole upr = userProjectRoleRepository.findByUserIdAndProjectId(userId, project.getId())
                                .orElseThrow(() -> AccessErrorCode.MEMBER_NOT_FOUND.toException(userId.toString()));

                userProjectRoleRepository.delete(upr);
                log.info("User {} removed from project {}", userId, projectCode);
        }

        private Project findProjectByCode(String projectCode) {
                return projectRepository.findByProjectCode(projectCode)
                                .orElseThrow(() -> ProjectErrorCode.PROJECT_NOT_FOUND.toException(projectCode));
        }

        private void checkRoleAssignmentPrivilege(ProjectRole roleToAssign) {
                if (ProjectRole.PROJECT_ADMIN == roleToAssign) {
                        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                        boolean isSystemAdmin = auth != null && auth.getAuthorities().stream()
                                        .anyMatch(a -> "SYSTEM_ADMIN".equals(a.getAuthority()));
                        if (!isSystemAdmin) {
                                throw AccessErrorCode.INVALID_ASSIGNMENT.toException("Only SYSTEM_ADMIN can assign PROJECT_ADMIN role");
                        }
                }
        }

        private ProjectMemberResponse toResponse(UserProjectRole upr) {
                User user = upr.getUser();
                return new ProjectMemberResponse(
                                user.getId(),
                                user.getUsername(),
                                user.getEmail(),
                                user.getFirstName(),
                                user.getLastName(),
                                upr.getRoleName().name(),
                                upr.getCreatedAt());
        }

        private ProjectRole parseRole(String roleIdOrName) {
                try {
                        int id = Integer.parseInt(roleIdOrName);
                        switch (id) {
                            case 1: return ProjectRole.PROJECT_ADMIN;
                            case 2: return ProjectRole.MAINTAINER;
                            case 3: return ProjectRole.DEVELOPER;
                            case 4: return ProjectRole.MAINTAINER;
                        }
                } catch (NumberFormatException e) {
                }
                try {
                        return ProjectRole.valueOf(roleIdOrName);
                } catch (Exception e) {
                        throw AccessErrorCode.ROLE_NOT_FOUND.toException();
                }
        }
}
