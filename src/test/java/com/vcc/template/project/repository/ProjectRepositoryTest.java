package com.vcc.template.project.repository;

import com.vcc.template.config.TestAuditingConfig;
import com.vcc.template.project.entity.Project;
import com.vcc.template.project.entity.ProjectStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for ProjectRepository.
 * Tests CRUD operations, unique constraints, and status filtering with pagination.
 * Uses Testcontainers to run tests against a real PostgreSQL database.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Transactional
@Import(TestAuditingConfig.class)
public class ProjectRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ProjectRepository projectRepository;

    @BeforeEach
    void setUp() {
        projectRepository.deleteAll();
    }

    // ========== CRUD Operations Tests ==========

    @Test
    void shouldCreateProject() {
        // Given
        Project project = createProject("test-project", "Test Project", ProjectStatus.ACTIVE);

        // When
        Project savedProject = projectRepository.save(project);

        // Then
        assertThat(savedProject.getId()).isNotNull();
        assertThat(savedProject.getProjectCode()).isEqualTo("test-project");
        assertThat(savedProject.getProjectName()).isEqualTo("Test Project");
        assertThat(savedProject.getStatus()).isEqualTo(ProjectStatus.ACTIVE);
        assertThat(savedProject.getCreatedAt()).isNotNull();
        assertThat(savedProject.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldReadProjectById() {
        // Given
        Project project = createProject("read-test", "Read Test Project", ProjectStatus.ACTIVE);
        Project savedProject = projectRepository.save(project);

        // When
        Optional<Project> foundProject = projectRepository.findById(savedProject.getId());

        // Then
        assertThat(foundProject).isPresent();
        assertThat(foundProject.get().getProjectCode()).isEqualTo("read-test");
        assertThat(foundProject.get().getProjectName()).isEqualTo("Read Test Project");
    }

    @Test
    void shouldUpdateProject() {
        // Given
        Project project = createProject("update-test", "Original Name", ProjectStatus.ACTIVE);
        Project savedProject = projectRepository.save(project);

        // When
        Project projectToUpdate = projectRepository.findById(savedProject.getId()).orElseThrow();
        projectToUpdate.setProjectName("Updated Name");
        projectToUpdate.setStatus(ProjectStatus.INACTIVE);
        Project updatedProject = projectRepository.save(projectToUpdate);

        // Then
        Project reloadedProject = projectRepository.findById(savedProject.getId()).orElseThrow();
        assertThat(reloadedProject.getProjectName()).isEqualTo("Updated Name");
        assertThat(reloadedProject.getStatus()).isEqualTo(ProjectStatus.INACTIVE);
        assertThat(reloadedProject.getProjectCode()).isEqualTo("update-test"); // Immutable
    }

    @Test
    void shouldDeleteProject() {
        // Given
        Project project = createProject("delete-test", "Delete Test Project", ProjectStatus.ACTIVE);
        Project savedProject = projectRepository.save(project);

        // When
        projectRepository.deleteById(savedProject.getId());

        // Then
        Optional<Project> deletedProject = projectRepository.findById(savedProject.getId());
        assertThat(deletedProject).isEmpty();
    }

    // ========== Unique Constraint Tests ==========

    @Test
    void shouldEnforceUniqueProjectCode() {
        // Given
        Project project1 = createProject("duplicate-code", "First Project", ProjectStatus.ACTIVE);
        projectRepository.save(project1);

        // When & Then
        Project project2 = createProject("duplicate-code", "Second Project", ProjectStatus.ACTIVE);
        assertThatThrownBy(() -> {
            projectRepository.save(project2);
            projectRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldAllowDifferentProjectCodes() {
        // Given
        Project project1 = createProject("project-one", "Project One", ProjectStatus.ACTIVE);
        Project project2 = createProject("project-two", "Project Two", ProjectStatus.ACTIVE);

        // When
        Project saved1 = projectRepository.save(project1);
        Project saved2 = projectRepository.save(project2);

        // Then
        assertThat(saved1.getId()).isNotNull();
        assertThat(saved2.getId()).isNotNull();
        assertThat(saved1.getId()).isNotEqualTo(saved2.getId());
    }

    // ========== Custom Query Method Tests ==========

    @Test
    void shouldFindByProjectCode() {
        // Given
        Project project = createProject("find-by-code", "Find By Code Project", ProjectStatus.ACTIVE);
        projectRepository.save(project);

        // When
        Optional<Project> foundProject = projectRepository.findByProjectCode("find-by-code");

        // Then
        assertThat(foundProject).isPresent();
        assertThat(foundProject.get().getProjectName()).isEqualTo("Find By Code Project");
    }

    @Test
    void shouldReturnEmptyWhenProjectCodeNotFound() {
        // When
        Optional<Project> foundProject = projectRepository.findByProjectCode("non-existent");

        // Then
        assertThat(foundProject).isEmpty();
    }

    @Test
    void shouldCheckExistsByProjectCode() {
        // Given
        Project project = createProject("exists-test", "Exists Test Project", ProjectStatus.ACTIVE);
        projectRepository.save(project);

        // When & Then
        assertThat(projectRepository.existsByProjectCode("exists-test")).isTrue();
        assertThat(projectRepository.existsByProjectCode("non-existent")).isFalse();
    }

    // ========== Status Filtering with Pagination Tests ==========

    @Test
    void shouldFindByStatusWithPagination() {
        // Given
        projectRepository.save(createProject("active-1", "Active Project 1", ProjectStatus.ACTIVE));
        projectRepository.save(createProject("active-2", "Active Project 2", ProjectStatus.ACTIVE));
        projectRepository.save(createProject("active-3", "Active Project 3", ProjectStatus.ACTIVE));
        projectRepository.save(createProject("inactive-1", "Inactive Project 1", ProjectStatus.INACTIVE));
        projectRepository.save(createProject("inactive-2", "Inactive Project 2", ProjectStatus.INACTIVE));

        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<Project> activePage = projectRepository.findByStatus(ProjectStatus.ACTIVE, pageable);
        Page<Project> inactivePage = projectRepository.findByStatus(ProjectStatus.INACTIVE, pageable);

        // Then
        assertThat(activePage.getTotalElements()).isEqualTo(3);
        assertThat(activePage.getContent()).hasSize(3);
        assertThat(activePage.getContent()).allMatch(p -> p.getStatus() == ProjectStatus.ACTIVE);

        assertThat(inactivePage.getTotalElements()).isEqualTo(2);
        assertThat(inactivePage.getContent()).hasSize(2);
        assertThat(inactivePage.getContent()).allMatch(p -> p.getStatus() == ProjectStatus.INACTIVE);
    }

    @Test
    void shouldPaginateCorrectly() {
        // Given - Create 5 active projects
        for (int i = 1; i <= 5; i++) {
            projectRepository.save(createProject("project-" + i, "Project " + i, ProjectStatus.ACTIVE));
        }

        // When - Request page 0 with size 2
        Pageable page0 = PageRequest.of(0, 2);
        Page<Project> firstPage = projectRepository.findByStatus(ProjectStatus.ACTIVE, page0);

        // Then
        assertThat(firstPage.getTotalElements()).isEqualTo(5);
        assertThat(firstPage.getTotalPages()).isEqualTo(3);
        assertThat(firstPage.getNumber()).isEqualTo(0);
        assertThat(firstPage.getSize()).isEqualTo(2);
        assertThat(firstPage.getContent()).hasSize(2);

        // When - Request page 1 with size 2
        Pageable page1 = PageRequest.of(1, 2);
        Page<Project> secondPage = projectRepository.findByStatus(ProjectStatus.ACTIVE, page1);

        // Then
        assertThat(secondPage.getNumber()).isEqualTo(1);
        assertThat(secondPage.getContent()).hasSize(2);

        // When - Request page 2 with size 2
        Pageable page2 = PageRequest.of(2, 2);
        Page<Project> thirdPage = projectRepository.findByStatus(ProjectStatus.ACTIVE, page2);

        // Then
        assertThat(thirdPage.getNumber()).isEqualTo(2);
        assertThat(thirdPage.getContent()).hasSize(1); // Last page has only 1 item
    }

    @Test
    void shouldReturnEmptyPageWhenNoMatchingStatus() {
        // Given
        projectRepository.save(createProject("active-only", "Active Only", ProjectStatus.ACTIVE));

        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<Project> inactivePage = projectRepository.findByStatus(ProjectStatus.INACTIVE, pageable);

        // Then
        assertThat(inactivePage.getTotalElements()).isZero();
        assertThat(inactivePage.getContent()).isEmpty();
    }

    // ========== Helper Methods ==========

    private Project createProject(String projectCode, String projectName, ProjectStatus status) {
        Project project = new Project();
        project.setProjectCode(projectCode);
        project.setProjectName(projectName);
        project.setStatus(status);
        return project;
    }
}
