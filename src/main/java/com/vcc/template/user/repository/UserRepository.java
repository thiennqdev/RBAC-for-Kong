package com.vcc.template.user.repository;

import com.vcc.template.common.repository.BaseRepository;
import com.vcc.template.user.entity.User;
import com.vcc.template.user.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends BaseRepository<User, Long> {

    Optional<User> findByKeycloakId(String keycloakId);

    @Query("SELECT u FROM User u WHERE u.keycloakId IS NULL OR u.keycloakId = ''")
    List<User> findUnsyncedUsers();

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    @Query("""
                SELECT u FROM User u
                WHERE (:keyword IS NULL OR :keyword = ''
                    OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')))
                AND (:status IS NULL OR u.status = :status)
            """)
    Page<User> search(@Param("keyword") String keyword,
            @Param("status") UserStatus status,
            Pageable pageable);
}
