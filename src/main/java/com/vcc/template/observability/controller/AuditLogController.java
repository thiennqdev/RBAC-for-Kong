package com.vcc.template.observability.controller;

import com.vcc.template.common.dto.PaginationResponse;
import com.vcc.template.observability.dto.AuditLogSearchRequest;
import com.vcc.template.observability.entity.AuditLog;
import com.vcc.template.observability.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/projects/{projectCode}/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("@projectSecurity.hasAnyRole(#projectCode, 'PROJECT_ADMIN', 'MAINTAINER', 'DEVELOPER')")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping(version = "1.0")
    public ResponseEntity<PaginationResponse<AuditLog>> searchLogs(
            @PathVariable String projectCode,
            @ModelAttribute AuditLogSearchRequest request) {
        
        // Passed projectCode to service because MongoDB uses it instead of TenantContext
        return ResponseEntity.ok(auditLogService.searchLogs(projectCode, request));
    }
}
