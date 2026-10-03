package com.vcc.template.observability.service;

import com.vcc.template.common.dto.PaginationResponse;
import com.vcc.template.common.util.RequestUtil;
import com.vcc.template.common.util.ResponseUtil;
import com.vcc.template.observability.dto.AuditLogSearchRequest;
import com.vcc.template.observability.entity.AuditLog;
import com.vcc.template.observability.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final MongoTemplate mongoTemplate;

    public PaginationResponse<AuditLog> searchLogs(String projectCode, AuditLogSearchRequest request) {
        Pageable pageable = RequestUtil.getPageable(request);

        Query query = new Query();
        query.addCriteria(Criteria.where("projectCode").is(projectCode));
        
        if (org.springframework.util.StringUtils.hasText(request.getAction())) {
            query.addCriteria(Criteria.where("action").is(request.getAction()));
        }
        if (org.springframework.util.StringUtils.hasText(request.getEntityName())) {
            query.addCriteria(Criteria.where("entityName").is(request.getEntityName()));
        }
        if (org.springframework.util.StringUtils.hasText(request.getActor())) {
            query.addCriteria(Criteria.where("actor").is(request.getActor()));
        }
        if (request.getFromDate() != null || request.getToDate() != null) {
            Criteria dateCriteria = Criteria.where("createdAt");
            if (request.getFromDate() != null) dateCriteria.gte(request.getFromDate());
            if (request.getToDate() != null) dateCriteria.lte(request.getToDate());
            query.addCriteria(dateCriteria);
        }

        long total = mongoTemplate.count(query, AuditLog.class);
        query.with(pageable);
        List<AuditLog> logs = mongoTemplate.find(query, AuditLog.class);

        return ResponseUtil.toPagingResponse(new PageImpl<>(logs, pageable, total));
    }

    public AuditLog saveAuditLog(AuditLog auditLog) {
        return auditLogRepository.save(auditLog);
    }

    public void deleteLogsByProjectCode(String projectCode) {
        auditLogRepository.deleteByProjectCode(projectCode);
    }
}
