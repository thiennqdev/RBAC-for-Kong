package com.vcc.template.observability.consumer;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.vcc.template.observability.dto.GatewayLogEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer that listens to Kong gateway log messages.
 *
 * <p>Kong's kafka-log plugin publishes request/response metadata to a Kafka topic.
 * This consumer parses the JSON payload, extracts the project code from the request URI
 * (based on the naming convention /{projectCode}/...), and pushes the log entry
 * to the appropriate WebSocket channel for live streaming.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaLogConsumer {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final com.vcc.template.observability.repository.GatewayLogRepository gatewayLogRepository;

    @KafkaListener(topics = "${app.kafka.topic.kong-logs:kong-log}", groupId = "rbac-log-consumer")
    public void consume(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);

            // Extract request URI to determine project code
            String uri = "";
            JsonNode requestNode = root.get("request");
            if (requestNode != null && requestNode.has("uri")) {
                uri = requestNode.get("uri").asText();
            }

            String projectCode = extractProjectCode(uri);
            if (projectCode == null || projectCode.isBlank()) {
                log.debug("Skipping log entry with unresolvable projectCode from URI: {}", uri);
                return;
            }

            // Build log entry DTO
            GatewayLogEntry entry = GatewayLogEntry.builder()
                    .projectCode(projectCode)
                    .method(getTextSafe(requestNode, "method"))
                    .uri(uri)
                    .statusCode(getIntSafe(root.get("response"), "status", 0))
                    .clientIp(getTextSafe(root, "client_ip"))
                    .timestamp(getTextSafe(root, "started_at"))
                    .build();

            // Extract latency info
            JsonNode latencies = root.get("latencies");
            if (latencies != null) {
                entry.setLatency(latencies.has("request") ? latencies.get("request").asLong() : 0);
                entry.setKongLatency(latencies.has("kong") ? latencies.get("kong").asLong() : 0);
                entry.setUpstreamLatency(latencies.has("proxy") ? latencies.get("proxy").asLong() : 0);
            }

            // Extract service info
            JsonNode serviceNode = root.get("service");
            if (serviceNode != null) {
                entry.setServiceName(getTextSafe(serviceNode, "name"));
            }

            JsonNode upstreamUriNode = root.get("upstream_uri");
            if (upstreamUriNode != null) {
                entry.setUpstreamUri(upstreamUriNode.asText());
            }

            // 1. Push to WebSocket topic for the specific project FIRST (fast, non-blocking)
            try {
                messagingTemplate.convertAndSend("/topic/logs." + projectCode, entry);
                log.debug("Pushed log entry to /topic/logs.{}: {} {} -> {}", 
                        projectCode, entry.getMethod(), uri, entry.getStatusCode());
            } catch (Exception e) {
                log.warn("Failed to push live log to WebSocket for project {}: {}", projectCode, e.getMessage());
            }

            // 2. Save to database SECOND
            com.vcc.template.observability.entity.GatewayLog dbLog = com.vcc.template.observability.entity.GatewayLog.builder()
                    .projectCode(projectCode)
                    .method(entry.getMethod())
                    .uri(entry.getUri())
                    .statusCode(entry.getStatusCode())
                    .clientIp(entry.getClientIp())
                    .latency(entry.getLatency())
                    .kongLatency(entry.getKongLatency())
                    .upstreamLatency(entry.getUpstreamLatency())
                    .serviceName(entry.getServiceName())
                    .upstreamUri(entry.getUpstreamUri())
                    .timestamp(entry.getTimestamp())
                    .build();

            try {
                gatewayLogRepository.save(dbLog);
            } catch (Exception e) {
                log.error("Failed to save gateway log to database for project {}: {}", projectCode, e.getMessage());
            }

        } catch (Exception e) {
            log.warn("Failed to process Kong log message: {}", e.getMessage(), e);
        }
    }

    /**
     * Extracts project code from URI based on naming convention.
     * URI format: /{projectCode}/rest/of/path
     */
    private String extractProjectCode(String uri) {
        if (uri == null || uri.isBlank()) return null;
        // Remove leading slash and split
        String clean = uri.startsWith("/") ? uri.substring(1) : uri;
        String[] segments = clean.split("/");
        
        // If the request is routed to the internal RBAC API, ignore it (we don't want gateway logs for it)
        if (segments.length >= 1 && "api".equals(segments[0])) {
            return null; // Ignore requests to /api/v1.0/... or /api/rbac/...
        }
        
        if (segments.length > 0) {
            return segments[0];
        }
        return null;
    }

    private String getTextSafe(JsonNode node, String field) {
        if (node != null && node.has(field)) {
            return node.get(field).asText();
        }
        return "";
    }

    private int getIntSafe(JsonNode node, String field, int defaultValue) {
        if (node != null && node.has(field)) {
            return node.get(field).asInt(defaultValue);
        }
        return defaultValue;
    }
}
