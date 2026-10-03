package com.vcc.template.observability.repository;

import com.vcc.template.observability.entity.GatewayLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GatewayLogRepository extends MongoRepository<GatewayLog, String> {

    Page<GatewayLog> findByProjectCode(String projectCode, Pageable pageable);
    void deleteByProjectCode(String projectCode);
}
