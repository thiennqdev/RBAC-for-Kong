package com.vcc.template.observability.repository;

import com.vcc.template.observability.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {

    Page<AuditLog> findByProjectCode(String projectCode, Pageable pageable);
    void deleteByProjectCode(String projectCode);
}
