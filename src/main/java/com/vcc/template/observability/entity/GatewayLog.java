package com.vcc.template.observability.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "gateway_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayLog {

    @Id
    private String id;

    @Indexed
    @Field("project_code")
    private String projectCode;

    private String method;

    private String uri;

    @Field("status_code")
    private Integer statusCode;

    @Field("client_ip")
    private String clientIp;

    private Long latency;

    @Field("kong_latency")
    private Long kongLatency;

    @Field("upstream_latency")
    private Long upstreamLatency;

    @Field("service_name")
    private String serviceName;

    @Field("upstream_uri")
    private String upstreamUri;

    private String timestamp;

    @CreatedDate
    @Indexed(expireAfterSeconds = 2592000) // TTL 30 days (30 * 24 * 60 * 60)
    @Field("created_at")
    private LocalDateTime createdAt;
}
