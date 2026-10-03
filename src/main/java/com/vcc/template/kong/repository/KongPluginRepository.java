package com.vcc.template.kong.repository;

import com.vcc.template.kong.entity.KongPluginEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;

@Repository
public interface KongPluginRepository extends JpaRepository<KongPluginEntity, Long> {
    List<KongPluginEntity> findByProjectIdAndKongInstanceId(Long projectId, Long kongInstanceId);
    Optional<KongPluginEntity> findByIdAndProjectIdAndKongInstanceId(Long id, Long projectId, Long kongInstanceId);
    List<KongPluginEntity> findByProjectIdAndScopeAndScopeEntityIdAndKongInstanceId(Long projectId, String scope, Long scopeEntityId, Long kongInstanceId);
    
    @Modifying
    @Query("DELETE FROM KongPluginEntity e WHERE e.project.id = :projectId")
    void deleteAllByProjectId(@Param("projectId") Long projectId);
}
