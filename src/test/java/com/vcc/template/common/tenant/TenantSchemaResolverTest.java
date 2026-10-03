package com.vcc.template.common.tenant;

import com.vcc.template.project.entity.Project;
import com.vcc.template.project.entity.ProjectStatus;
import com.vcc.template.project.repository.ProjectRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for TenantSchemaResolver.
 */
@ExtendWith(MockitoExtension.class)
class TenantSchemaResolverTest {

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private ProjectRepository projectRepository;

    private TenantSchemaResolver tenantSchemaResolver;

    @BeforeEach
    void setUp() {
        tenantSchemaResolver = new TenantSchemaResolver();
        tenantSchemaResolver.setApplicationContext(applicationContext);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldReturnDefaultTenantWhenNoTenantInContext() {
        // Given: No tenant in context
        TenantContext.clear();

        // When
        String result = tenantSchemaResolver.resolveCurrentTenantIdentifier();

        // Then
        assertThat(result).isEqualTo(TenantSchemaResolver.DEFAULT_TENANT);
    }

    @Test
    void shouldReturnDefaultTenantWhenTenantIsAlreadyDefault() {
        // Given: Default tenant in context
        TenantContext.setCurrentTenant(TenantSchemaResolver.DEFAULT_TENANT);

        // When
        String result = tenantSchemaResolver.resolveCurrentTenantIdentifier();

        // Then
        assertThat(result).isEqualTo(TenantSchemaResolver.DEFAULT_TENANT);
    }

    @Test
    void shouldReturnTenantWhenProjectIsActive() {
        // Given: Active project exists
        String tenantId = "active-project";
        TenantContext.setCurrentTenant(tenantId);
        
        // Mock the repository lookup
        when(applicationContext.getBean(ProjectRepository.class)).thenReturn(projectRepository);
        
        Project activeProject = new Project();
        activeProject.setProjectCode(tenantId);
        activeProject.setStatus(ProjectStatus.ACTIVE);
        
        when(projectRepository.findByProjectCode(tenantId))
                .thenReturn(Optional.of(activeProject));

        // When
        String result = tenantSchemaResolver.resolveCurrentTenantIdentifier();

        // Then
        assertThat(result).isEqualTo(tenantId);
    }

    @Test
    void shouldReturnDefaultTenantWhenProjectIsInactive() {
        // Given: Inactive project exists
        String tenantId = "inactive-project";
        TenantContext.setCurrentTenant(tenantId);
        
        // Mock the repository lookup
        when(applicationContext.getBean(ProjectRepository.class)).thenReturn(projectRepository);
        
        Project inactiveProject = new Project();
        inactiveProject.setProjectCode(tenantId);
        inactiveProject.setStatus(ProjectStatus.INACTIVE);
        
        when(projectRepository.findByProjectCode(tenantId))
                .thenReturn(Optional.of(inactiveProject));

        // When
        String result = tenantSchemaResolver.resolveCurrentTenantIdentifier();

        // Then
        assertThat(result).isEqualTo(TenantSchemaResolver.DEFAULT_TENANT);
    }

    @Test
    void shouldReturnDefaultTenantWhenProjectNotFound() {
        // Given: Project does not exist
        String tenantId = "non-existent-project";
        TenantContext.setCurrentTenant(tenantId);
        
        // Mock the repository lookup
        when(applicationContext.getBean(ProjectRepository.class)).thenReturn(projectRepository);
        
        when(projectRepository.findByProjectCode(tenantId))
                .thenReturn(Optional.empty());

        // When
        String result = tenantSchemaResolver.resolveCurrentTenantIdentifier();

        // Then
        assertThat(result).isEqualTo(TenantSchemaResolver.DEFAULT_TENANT);
    }

    @Test
    void shouldReturnDefaultTenantWhenRepositoryThrowsException() {
        // Given: Repository throws exception
        String tenantId = "error-project";
        TenantContext.setCurrentTenant(tenantId);
        
        // Mock the repository lookup
        when(applicationContext.getBean(ProjectRepository.class)).thenReturn(projectRepository);
        
        when(projectRepository.findByProjectCode(tenantId))
                .thenThrow(new RuntimeException("Database error"));

        // When
        String result = tenantSchemaResolver.resolveCurrentTenantIdentifier();

        // Then
        assertThat(result).isEqualTo(TenantSchemaResolver.DEFAULT_TENANT);
    }
}