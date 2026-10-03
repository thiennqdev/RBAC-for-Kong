package com.vcc.template.observability.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO representing a single gateway log entry pushed to frontend via WebSocket.
 * Contains key fields extracted from Kong's HTTP Log plugin output.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GatewayLogEntry {
    private String projectCode;
    private String method;
    private String uri;
    private int statusCode;
    private long latency;        // Total request latency in ms
    private long kongLatency;    // Kong processing latency in ms
    private long upstreamLatency; // Upstream (backend) latency in ms
    private String clientIp;
    private String timestamp;
    private String upstreamUri;
    private String serviceName;
}
