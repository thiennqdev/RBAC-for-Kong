package com.vcc.template.observability.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Proxy service that queries Prometheus HTTP API on behalf of the frontend.
 * This keeps the Prometheus instance internal and not exposed to end-users.
 */
@Service
@Slf4j
public class PrometheusService {

    private final RestClient restClient;

    public PrometheusService(@Value("${prometheus.url:http://localhost:9090}") String prometheusUrl,
                             RestClient.Builder builder) {
        this.restClient = builder.baseUrl(prometheusUrl).build();
        log.info("PrometheusService initialized with URL: {}", prometheusUrl);
    }

    /**
     * Instant query – GET /api/v1/query?query=...
     * Returns the raw Prometheus JSON response as a Map.
     */
    public Map<String, Object> query(String promQL) {
        log.debug("Prometheus instant query: {}", promQL);
        return restClient.get()
                .uri("/api/v1/query?query={query}", promQL)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    /**
     * Range query – GET /api/v1/query_range?query=...&start=...&end=...&step=...
     * Returns time-series data suitable for charting.
     *
     * @param promQL the PromQL expression
     * @param start  epoch seconds (e.g. Unix timestamp)
     * @param end    epoch seconds
     * @param step   resolution in seconds (e.g. "60" for 1-minute buckets)
     */
    public Map<String, Object> queryRange(String promQL, long start, long end, String step) {
        log.debug("Prometheus range query: {} [{} → {}] step={}", promQL, start, end, step);
        return restClient.get()
                .uri("/api/v1/query_range?query={query}&start={start}&end={end}&step={step}", 
                     promQL, start, end, step)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
