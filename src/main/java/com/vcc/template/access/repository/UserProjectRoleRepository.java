package com.vcc.template.access.repository;

import com.vcc.template.access.entity.UserProjectRole;
import com.vcc.template.common.repository.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProjectRoleRepository extends BaseRepository<UserProjectRole, Long> {

    @Query("""
                SELECT upr FROM UserProjectRole upr
                JOIN FETCH upr.user
                WHERE upr.project.id = :projectId
                ORDER BY upr.user.username
            """)
    List<UserProjectRole> findByProjectIdWithDetails(@Param("projectId") Long projectId);

    Optional<UserProjectRole> findByUserIdAndProjectId(Long userId, Long projectId);

    boolean existsByUserIdAndProjectId(Long userId, Long projectId);


    @Query("""
                SELECT upr FROM UserProjectRole upr
                WHERE upr.user.id = :userId AND upr.project.projectCode = :projectCode
            """)
    Optional<UserProjectRole> findByUserIdAndProjectCode(@Param("userId") Long userId,
            @Param("projectCode") String projectCode);

    List<UserProjectRole> findByProjectProjectCodeAndRoleName(String projectCode, com.vcc.template.access.model.ProjectRole roleName);
}

