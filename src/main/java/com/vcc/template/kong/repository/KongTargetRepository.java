package com.vcc.template.kong.repository;

import com.vcc.template.kong.entity.KongTargetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;

@Repository
public interface KongTargetRepository extends JpaRepository<KongTargetEntity, Long> {
    List<KongTargetEntity> findByKongUpstreamIdAndProjectIdAndKongInstanceId(Long kongUpstreamId, Long projectId, Long kongInstanceId);
    Optional<KongTargetEntity> findByIdAndProjectIdAndKongInstanceId(Long id, Long projectId, Long kongInstanceId);

    @Modifying
    @Query("DELETE FROM KongTargetEntity e WHERE e.kongUpstream.id = :kongUpstreamId")
    void deleteAllByKongUpstreamId(@Param("kongUpstreamId") Long kongUpstreamId);

    @Modifying
    @Query("DELETE FROM KongTargetEntity e WHERE e.project.id = :projectId")
    void deleteAllByProjectId(@Param("projectId") Long projectId);
}
