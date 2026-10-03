package com.vcc.template.observability.service;

import com.vcc.template.common.dto.PaginationResponse;
import com.vcc.template.common.util.RequestUtil;
import com.vcc.template.common.util.ResponseUtil;
import com.vcc.template.observability.dto.GatewayLogSearchRequest;
import com.vcc.template.observability.entity.GatewayLog;
import com.vcc.template.observability.repository.GatewayLogRepository;
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
public class GatewayLogService {

    private final GatewayLogRepository gatewayLogRepository;
    private final MongoTemplate mongoTemplate;

    public PaginationResponse<GatewayLog> searchLogs(String projectCode, GatewayLogSearchRequest request) {
        Pageable pageable = RequestUtil.getPageable(request);

        Query query = new Query();
        query.addCriteria(Criteria.where("projectCode").is(projectCode));

        if (org.springframework.util.StringUtils.hasText(request.getMethod())) {
            query.addCriteria(Criteria.where("method").is(request.getMethod()));
        }
        if (request.getStatusCode() != null) {
            query.addCriteria(Criteria.where("statusCode").is(request.getStatusCode()));
        }
        if (org.springframework.util.StringUtils.hasText(request.getClientIp())) {
            query.addCriteria(Criteria.where("clientIp").is(request.getClientIp()));
        }
        if (org.springframework.util.StringUtils.hasText(request.getUri())) {
            query.addCriteria(Criteria.where("uri").regex(request.getUri(), "i"));
        }
        if (request.getFromDate() != null || request.getToDate() != null) {
            Criteria dateCriteria = Criteria.where("createdAt");
            if (request.getFromDate() != null) dateCriteria.gte(request.getFromDate());
            if (request.getToDate() != null) dateCriteria.lte(request.getToDate());
            query.addCriteria(dateCriteria);
        }

        long total = mongoTemplate.count(query, GatewayLog.class);
        query.with(pageable);
        List<GatewayLog> logs = mongoTemplate.find(query, GatewayLog.class);

        return ResponseUtil.toPagingResponse(new PageImpl<>(logs, pageable, total));
    }

    public void deleteLogsByProjectCode(String projectCode) {
        gatewayLogRepository.deleteByProjectCode(projectCode);
    }
}
