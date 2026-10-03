package com.vcc.template.kong.repository;

import com.vcc.template.kong.entity.KongInstanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KongInstanceRepository extends JpaRepository<KongInstanceEntity, Long> {
    Optional<KongInstanceEntity> findByCode(String code);
}
