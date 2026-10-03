package com.vcc.template.kong.repository;

import com.vcc.template.kong.entity.KongRouteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;

@Repository
public interface KongRouteRepository extends JpaRepository<KongRouteEntity, Long> {
    List<KongRouteEntity> findByProjectIdAndKongServiceIdAndKongInstanceId(Long projectId, Long kongServiceId, Long kongInstanceId);
    Optional<KongRouteEntity> findByIdAndProjectIdAndKongInstanceId(Long id, Long projectId, Long kongInstanceId);
    Optional<KongRouteEntity> findByIdAndProjectIdAndKongServiceIdAndKongInstanceId(Long id, Long projectId, Long kongServiceId, Long kongInstanceId);

    @Modifying
    @Query("DELETE FROM KongRouteEntity e WHERE e.project.id = :projectId")
    void deleteAllByProjectId(@Param("projectId") Long projectId);
}
