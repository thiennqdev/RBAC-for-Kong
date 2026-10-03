package com.vcc.template.kong.repository;

import com.vcc.template.kong.entity.KongServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;

@Repository
public interface KongServiceRepository extends JpaRepository<KongServiceEntity, Long> {
    List<KongServiceEntity> findByProjectId(Long projectId);
    List<KongServiceEntity> findByProjectIdAndKongInstanceId(Long projectId, Long kongInstanceId);
    Optional<KongServiceEntity> findByIdAndProjectIdAndKongInstanceId(Long id, Long projectId, Long kongInstanceId);
    boolean existsByKongId(String kongId);

    @Modifying
    @Query("DELETE FROM KongServiceEntity e WHERE e.project.id = :projectId")
    void deleteAllByProjectId(@Param("projectId") Long projectId);
}
