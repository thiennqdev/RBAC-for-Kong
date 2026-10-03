package com.vcc.template.observability.controller;

import com.vcc.template.observability.service.PrometheusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Secure proxy endpoint for Prometheus metrics.
 * Tenant isolation is enforced by the existing ProjectTenantFilter
 * which validates that the authenticated user has access to the given projectCode.
 */
@RestController
@RequestMapping("/projects/{projectCode}/metrics")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("@projectSecurity.hasAnyRole(#projectCode, 'PROJECT_ADMIN', 'MAINTAINER', 'DEVELOPER')")
public class MetricsController {

    private final PrometheusService prometheusService;

    /**
     * Instant query – returns the current value of a PromQL expression.
     * Example: GET /projects/demo/metrics/query?query=kong_http_requests_total
     */
    @GetMapping(path = "/query", version = "1.0")
    public ResponseEntity<Map<String, Object>> instantQuery(
            @PathVariable String projectCode,
            @RequestParam String query) {

        log.debug("[{}] Metrics instant query: {}", projectCode, query);
        Map<String, Object> result = prometheusService.query(query);
        return ResponseEntity.ok(result);
    }

    /**
     * Range query – returns time-series data for charting.
     * Example: GET /projects/demo/metrics/query_range?query=...&start=...&end=...&step=60
     */
    @GetMapping(path = "/query_range", version = "1.0")
    public ResponseEntity<Map<String, Object>> rangeQuery(
            @PathVariable String projectCode,
            @RequestParam String query,
            @RequestParam long start,
            @RequestParam long end,
            @RequestParam(defaultValue = "60") String step) {

        log.debug("[{}] Metrics range query: {} [{} → {}] step={}", projectCode, query, start, end, step);
        Map<String, Object> result = prometheusService.queryRange(query, start, end, step);
        return ResponseEntity.ok(result);
    }
}
