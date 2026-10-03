package com.vcc.template.project.repository;

import com.vcc.template.common.repository.BaseRepository;
import com.vcc.template.project.dto.ProjectListResponse;
import com.vcc.template.project.dto.ProjectSearchRequest;
import com.vcc.template.project.entity.Project;
import com.vcc.template.project.entity.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Repository interface for Project entity operations.
 * Provides data access methods for project management.
 */
public interface ProjectRepository extends BaseRepository<Project, Long> {

  /**
   * Find a project by its unique project code.
   *
   * @param projectCode the project code to search for
   * @return an Optional containing the project if found, empty otherwise
   */
  Optional<Project> findByProjectCode(String projectCode);

  /**
   * Check if a project exists with the given project code.
   *
   * @param projectCode the project code to check
   * @return true if a project exists with the given code, false otherwise
   */
  boolean existsByProjectCode(String projectCode);

  /**
   * Find all projects with the specified status, with pagination support.
   *
   * @param status   the project status to filter by
   * @param pageable pagination information
   * @return a page of projects matching the status
   */
  Page<Project> findByStatus(ProjectStatus status, Pageable pageable);

  @Query("""
      SELECT new com.vcc.template.project.dto.ProjectListResponse(p.projectCode, p.projectName, p.iconPath, p.status, p.createdAt, p.createdBy, p.updatedAt, p.updatedBy, p.idpType, p.idpIssuerUrl, p.idpJwksUrl, p.idpClientId)
      FROM Project p
      WHERE (:#{#request.status} IS NULL OR p.status = :#{#request.status})
        AND (:#{#request.projectName} IS NULL OR LOWER(p.projectName) LIKE LOWER(CONCAT('%', :#{#request.projectName}, '%')))
        AND (:#{#request.projectCode} IS NULL OR LOWER(p.projectCode) LIKE LOWER(CONCAT('%', :#{#request.projectCode}, '%')))
      """)
  Page<ProjectListResponse> search(ProjectSearchRequest request, Pageable pageable);

  @Query("""
      SELECT new com.vcc.template.project.dto.ProjectListResponse(p.projectCode, p.projectName, p.iconPath, p.status, p.createdAt, p.createdBy, p.updatedAt, p.updatedBy, p.idpType, p.idpIssuerUrl, p.idpJwksUrl, p.idpClientId)
      FROM Project p
      JOIN UserProjectRole upr ON upr.project.id = p.id
      WHERE upr.user.id = :userId
        AND (:#{#request.status} IS NULL OR p.status = :#{#request.status})
        AND (:#{#request.projectName} IS NULL OR LOWER(p.projectName) LIKE LOWER(CONCAT('%', :#{#request.projectName}, '%')))
        AND (:#{#request.projectCode} IS NULL OR LOWER(p.projectCode) LIKE LOWER(CONCAT('%', :#{#request.projectCode}, '%')))
      """)
  Page<ProjectListResponse> searchByUserId(@Param("userId") Long userId, @Param("request") ProjectSearchRequest request,
      Pageable pageable);
}
