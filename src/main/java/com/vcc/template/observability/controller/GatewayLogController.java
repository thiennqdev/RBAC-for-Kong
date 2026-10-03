package com.vcc.template.observability.controller;

import com.vcc.template.common.dto.PaginationResponse;
import com.vcc.template.observability.dto.GatewayLogSearchRequest;
import com.vcc.template.observability.entity.GatewayLog;
import com.vcc.template.observability.service.GatewayLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/projects/{projectCode}/gateway-logs")
@RequiredArgsConstructor
@PreAuthorize("@projectSecurity.hasAnyRole(#projectCode, 'PROJECT_ADMIN', 'MAINTAINER', 'DEVELOPER')")
public class GatewayLogController {

    private final GatewayLogService gatewayLogService;

    @PostMapping(path = "/search", version = "1.0")
    public ResponseEntity<PaginationResponse<GatewayLog>> searchLogs(
            @PathVariable String projectCode,
            @RequestBody GatewayLogSearchRequest request) {
        
        // Passed projectCode to service because MongoDB uses it instead of TenantContext
        return ResponseEntity.ok(gatewayLogService.searchLogs(projectCode, request));
    }
}
