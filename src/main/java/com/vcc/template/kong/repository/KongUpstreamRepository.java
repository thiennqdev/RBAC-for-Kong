package com.vcc.template.kong.repository;

import com.vcc.template.kong.entity.KongUpstreamEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;

@Repository
public interface KongUpstreamRepository extends JpaRepository<KongUpstreamEntity, Long> {
    List<KongUpstreamEntity> findByProjectIdAndKongInstanceId(Long projectId, Long kongInstanceId);
    Optional<KongUpstreamEntity> findByIdAndProjectIdAndKongInstanceId(Long id, Long projectId, Long kongInstanceId);

    @Modifying
    @Query("DELETE FROM KongUpstreamEntity e WHERE e.project.id = :projectId")
    void deleteAllByProjectId(@Param("projectId") Long projectId);
}
