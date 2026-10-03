package com.vcc.template.storage.repository;

import com.vcc.template.storage.entity.ProjectStorageConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProjectStorageConfigRepository extends JpaRepository<ProjectStorageConfig, Long> {
    Optional<ProjectStorageConfig> findByProject_ProjectCode(String projectCode);
    boolean existsByProject_ProjectCode(String projectCode);
    Optional<ProjectStorageConfig> findByProjectId(Long projectId);
}
