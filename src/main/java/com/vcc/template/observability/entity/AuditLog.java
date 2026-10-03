package com.vcc.template.observability.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "audit_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    private String id;

    @Indexed
    @Field("project_code")
    private String projectCode;

    private String action; // CREATE, UPDATE, DELETE

    @Field("entity_name")
    private String entityName; // PROJECT, ROUTE, SERVICE, PLUGIN, UPSTREAM

    @Field("entity_id")
    private String entityId; // The ID or code of the resource

    private String actor; // Keycloak ID or Username

    @Field("old_value")
    private String oldValue; // Optional JSON

    @Field("new_value")
    private String newValue; // Optional JSON

    @Field("ip_address")
    private String ipAddress;

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;
}
