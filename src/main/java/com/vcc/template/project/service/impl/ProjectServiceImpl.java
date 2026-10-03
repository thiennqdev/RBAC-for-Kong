package com.vcc.template.project.service.impl;

import com.vcc.template.common.dto.PaginationResponse;
import com.vcc.template.common.file.service.FileStorageService;
import com.vcc.template.common.util.RequestUtil;
import com.vcc.template.common.util.ResponseUtil;
import com.vcc.template.project.dto.*;
import com.vcc.template.project.entity.Project;
import com.vcc.template.project.entity.ProjectStatus;
import com.vcc.template.project.errorcode.ProjectErrorCode;
import com.vcc.template.project.mapper.ProjectMapper;
import com.vcc.template.project.repository.ProjectRepository;
import com.vcc.template.project.schema.LiquibaseMigrationRunner;
import com.vcc.template.project.schema.SchemaManager;
import com.vcc.template.project.service.ProjectService;
import com.vcc.template.kong.service.KongJwtProvisioningService;
import com.vcc.template.common.keycloak.KeycloakAdminService;
import com.vcc.template.storage.service.ProjectStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import com.vcc.template.user.repository.UserRepository;
/**
 * Implementation of ProjectService interface.
 * Handles all business logic for project management operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;
    private final FileStorageService fileStorageService;
    private final SchemaManager schemaManager;
    private final LiquibaseMigrationRunner liquibaseMigrationRunner;
    private final KongJwtProvisioningService kongJwtProvisioningService;
    private final com.vcc.template.kong.repository.KongInstanceRepository kongInstanceRepository;
    private final KeycloakAdminService keycloakAdminService;
    private final ProjectStorageService projectStorageService;
    private final UserRepository userRepository;
    private final com.vcc.template.observability.service.AuditLogService auditLogService;
    private final com.vcc.template.observability.service.GatewayLogService gatewayLogService;
    @Override
    @CacheEvict(value = {"projects", "project-exists"}, allEntries = true)
    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request) {
        log.debug("Creating project with code: {}", request.projectCode());

        // 1. Validate projectCode uniqueness
        if (projectRepository.existsByProjectCode(request.projectCode())) {
            log.warn("Project creation failed: project code {} already exists", request.projectCode());
            throw ProjectErrorCode.PROJECT_ALREADY_EXISTS.toException(request.projectCode());
        }

        // 2. Validate iconPath exists in storage (if provided)
        if (request.iconPath() != null && !request.iconPath().isBlank()) {
            if (!fileStorageService.fileExists(request.iconPath())) {
                log.warn("Project creation failed: icon path {} does not exist", request.iconPath());
                throw ProjectErrorCode.FILE_NOT_FOUND.toException(request.iconPath());
            }
        }

        // 3. Validate schema name
        schemaManager.validateSchemaName(request.projectCode());

        // 4. Create Project entity with ACTIVE status
        Project project = projectMapper.toEntity(request);
        project.setStatus(ProjectStatus.ACTIVE);
        if (request.jwtAuthEnabled() == null) {
            project.setJwtAuthEnabled(true);
        }

        // Auto-generate Client ID for DEFAULT IdP projects
        if ("DEFAULT".equals(project.getIdpType())) {
            String autoClientId = "client-" + project.getProjectCode();
            try {
                String autoClientSecret = keycloakAdminService.createClient(autoClientId);
                project.setIdpClientId(autoClientId);
                project.setIdpClientSecret(autoClientSecret);
                log.info("Auto-provisioned Keycloak client '{}' for DEFAULT IdP project {}", autoClientId, project.getProjectCode());
            } catch (Exception e) {
                log.error("Failed to auto-provision Keycloak client for project {}: {}", project.getProjectCode(), e.getMessage());
                // We don't fail the whole creation, but it will affect Kong ACL later
            }
        }

        try {
            // 5. Create PostgreSQL schema
            log.debug("Creating schema for project: {}", request.projectCode());
            schemaManager.createSchema(request.projectCode());

            // 6. Execute Liquibase migrations
            log.debug("Running migrations for schema: {}", request.projectCode());
            liquibaseMigrationRunner.runMigrations(request.projectCode());

            // 7. Verify schema exists (using normalized name)
            if (!schemaManager.schemaExists(request.projectCode())) {
                log.error("Schema verification failed for project: {}", request.projectCode());
                throw ProjectErrorCode.SCHEMA_CREATION_FAILED.toException(request.projectCode());
            }

            // 8. Persist Project entity
            Project savedProject = projectRepository.save(project);

            // 9. Auto-provision Kong Consumer & JWT Credential across all Kong instances
            try {
                for (com.vcc.template.kong.entity.KongInstanceEntity instance : kongInstanceRepository.findAll()) {
                    try {
                        kongJwtProvisioningService.provisionJwtForProject(instance.getId(), savedProject.getProjectCode());
                    } catch (Exception e) {
                        log.error("Failed to provision Kong JWT for project {} on instance {}: {}", request.projectCode(), instance.getId(), e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.error("Failed to list Kong instances to provision JWT for project {}: {}", request.projectCode(), e.getMessage());
            }

            return projectMapper.toResponse(savedProject);

        } catch (Exception e) {
            log.error("Failed to create project {}: {}", request.projectCode(), e.getMessage(), e);
            throw ProjectErrorCode.SCHEMA_CREATION_FAILED.toException(request.projectCode());
        }
    }

    @Override
    @Cacheable(value = "projects", keyGenerator = "tenantAwareCacheKeyGenerator")
    @Transactional(readOnly = true)
    public ProjectResponse getProject(String projectCode) {
        log.debug("Retrieving project: {}", projectCode);

        Project project = projectRepository.findByProjectCode(projectCode)
                .orElseThrow(() -> {
                    log.warn("Project not found: {}", projectCode);
                    return ProjectErrorCode.PROJECT_NOT_FOUND.toException(projectCode);
                });

        return projectMapper.toResponse(project);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<ProjectListResponse> listProjects(ProjectSearchRequest request) {
        Pageable pageable = RequestUtil.getPageable(request);
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            return ResponseUtil.toPagingResponse(Page.empty(pageable));
        }

        if (auth.getAuthorities().stream().anyMatch(a -> "SYSTEM_ADMIN".equals(a.getAuthority()))) {
            Page<ProjectListResponse> responses = projectRepository.search(request, pageable);
            return ResponseUtil.toPagingResponse(responses);
        }

        String username = auth.getName();
        com.vcc.template.user.entity.User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return ResponseUtil.toPagingResponse(Page.empty(pageable));
        }

        Page<ProjectListResponse> responses = projectRepository.searchByUserId(user.getId(), request, pageable);
        return ResponseUtil.toPagingResponse(responses);
    }

    @Override
    @Cacheable(value = "project-exists", keyGenerator = "tenantAwareCacheKeyGenerator")
    @Transactional(readOnly = true)
    public boolean existsByProjectCode(String projectCode) {
        log.debug("Checking if project exists: {}", projectCode);
        return projectRepository.existsByProjectCode(projectCode);
    }

    @Override
    @CacheEvict(value = {"projects", "project-exists"}, allEntries = true)
    @Transactional
    public ProjectResponse updateProject(String projectCode, UpdateProjectRequest request) {
        log.debug("Updating project: {}", projectCode);

        // 1. Fetch existing project or throw PROJECT_NOT_FOUND
        Project project = projectRepository.findByProjectCode(projectCode)
                .orElseThrow(() -> {
                    log.warn("Project not found for update: {}", projectCode);
                    return ProjectErrorCode.PROJECT_NOT_FOUND.toException(projectCode);
                });

        // 2. Validate new iconPath exists in storage (if provided and changed)
        String oldIconPath = project.getIconPath();
        String newIconPath = request.iconPath();

        if (newIconPath != null && !newIconPath.isBlank()) {
            if (!fileStorageService.fileExists(newIconPath)) {
                log.warn("Project update failed: icon path {} does not exist", newIconPath);
                throw ProjectErrorCode.FILE_NOT_FOUND.toException(newIconPath);
            }
        }

        // 3. Update mutable fields
        project.setProjectName(request.projectName());
        project.setStatus(request.status());
        project.setIconPath(newIconPath);
        
        if (request.idpType() != null) project.setIdpType(request.idpType());
        if (request.idpIssuerUrl() != null) project.setIdpIssuerUrl(request.idpIssuerUrl());
        if (request.idpJwksUrl() != null) project.setIdpJwksUrl(request.idpJwksUrl());
        if (request.idpClientId() != null) project.setIdpClientId(request.idpClientId());
        if (request.idpClientSecret() != null) project.setIdpClientSecret(request.idpClientSecret());

        boolean oldJwtAuthEnabled = Boolean.TRUE.equals(project.getJwtAuthEnabled());
        if (request.jwtAuthEnabled() != null) {
            project.setJwtAuthEnabled(request.jwtAuthEnabled());
        }
        boolean newJwtAuthEnabled = Boolean.TRUE.equals(project.getJwtAuthEnabled());

        // 4. Delete old icon from storage if iconPath changed
        if (oldIconPath != null && !oldIconPath.isBlank() &&
                !oldIconPath.equals(newIconPath)) {
            try {
                log.debug("Deleting old icon: {}", oldIconPath);
                fileStorageService.deleteFile(oldIconPath);
            } catch (Exception e) {
                log.warn("Failed to delete old icon {}: {}", oldIconPath, e.getMessage());
                // Continue with update even if deletion fails
            }
        }

        // Auto-generate Client ID if switching to DEFAULT IdP and doesn't have one
        if ("DEFAULT".equals(project.getIdpType()) && 
            (project.getIdpClientId() == null || project.getIdpClientId().isBlank() || project.getIdpClientId().equals("rbac-admin-frontend"))) {
            String autoClientId = "client-" + project.getProjectCode();
            try {
                String autoClientSecret = keycloakAdminService.createClient(autoClientId);
                project.setIdpClientId(autoClientId);
                project.setIdpClientSecret(autoClientSecret);
                log.info("Auto-provisioned Keycloak client '{}' for DEFAULT IdP project {} during update", autoClientId, project.getProjectCode());
            } catch (Exception e) {
                log.error("Failed to auto-provision Keycloak client for project {}: {}", project.getProjectCode(), e.getMessage());
            }
        }

        // 5. Persist changes (audit fields updated automatically by JPA)
        Project updatedProject = projectRepository.save(project);
        
        // 6. Sync Kong Consumer & JWT Credential for this project across all instances
        try {
            for (com.vcc.template.kong.entity.KongInstanceEntity instance : kongInstanceRepository.findAll()) {
                try {
                    kongJwtProvisioningService.provisionJwtForProject(instance.getId(), updatedProject.getProjectCode());
                } catch (Exception e) {
                    log.error("Failed to re-provision Kong JWT for project {} on instance {}: {}", projectCode, instance.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Failed to list Kong instances to re-provision JWT for project {}: {}", projectCode, e.getMessage());
        }

        // 7. Sync Kong Services JWT plugins if toggle changed
        if (oldJwtAuthEnabled != newJwtAuthEnabled) {
            try {
                kongJwtProvisioningService.syncJwtAuthForProject(projectCode, newJwtAuthEnabled);
            } catch (Exception e) {
                log.error("Failed to sync Kong JWT plugins for project {}: {}", projectCode, e.getMessage());
            }
        }

        log.info("Project updated successfully: {}", updatedProject.getProjectCode());

        return projectMapper.toResponse(updatedProject);
    }

    @Override
    @CacheEvict(value = {"projects", "project-exists"}, allEntries = true)
    @Transactional
    public void deleteProject(String projectCode) {
        log.debug("Soft deleting project: {}", projectCode);

        // 1. Fetch existing project or throw PROJECT_NOT_FOUND
        Project project = projectRepository.findByProjectCode(projectCode)
                .orElseThrow(() -> {
                    log.warn("Project not found for deletion: {}", projectCode);
                    return ProjectErrorCode.PROJECT_NOT_FOUND.toException(projectCode);
                });

        // 2. Set status to INACTIVE (soft delete)
        project.setStatus(ProjectStatus.INACTIVE);

        // 3. Persist changes (audit fields updated automatically by JPA)
        projectRepository.save(project);
        log.info("Project soft deleted successfully: {}", projectCode);
    }

    @Override
    @CacheEvict(value = {"projects", "project-exists"}, allEntries = true)
    @Transactional
    public void hardDeleteProject(String projectCode) {
        log.debug("Hard deleting project: {}", projectCode);

        Project project = projectRepository.findByProjectCode(projectCode)
                .orElseThrow(() -> {
                    log.warn("Project not found for hard deletion: {}", projectCode);
                    return ProjectErrorCode.PROJECT_NOT_FOUND.toException(projectCode);
                });

        try {
            schemaManager.dropSchema(projectCode);
        } catch (Exception e) {
            log.error("Failed to drop schema for project {}: {}", projectCode, e.getMessage(), e);
        }

        try {
            projectStorageService.deleteStorageForProject(project.getId());
        } catch (Exception e) {
            log.error("Failed to delete storage for project {}: {}", projectCode, e.getMessage(), e);
        }

        try {
            auditLogService.deleteLogsByProjectCode(projectCode);
            gatewayLogService.deleteLogsByProjectCode(projectCode);
        } catch (Exception e) {
            log.error("Failed to delete MongoDB logs for project {}: {}", projectCode, e.getMessage(), e);
        }

        projectRepository.delete(project);
        log.info("Project hard deleted successfully: {}", projectCode);
    }

    @Override
    @CacheEvict(value = {"projects", "project-exists"}, allEntries = true)
    @Transactional
    public ProjectResponse reactivateProject(String projectCode) {
        log.debug("Reactivating project: {}", projectCode);

        Project project = projectRepository.findByProjectCode(projectCode)
                .orElseThrow(() -> {
                    log.warn("Project not found for reactivation: {}", projectCode);
                    return ProjectErrorCode.PROJECT_NOT_FOUND.toException(projectCode);
                });

        project.setStatus(ProjectStatus.ACTIVE);

        Project updatedProject = projectRepository.save(project);
        log.info("Project reactivated successfully: {}", projectCode);

        return projectMapper.toResponse(updatedProject);
    }
}
