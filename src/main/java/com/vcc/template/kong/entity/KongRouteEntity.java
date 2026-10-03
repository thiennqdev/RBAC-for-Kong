package com.vcc.template.kong.entity;

import com.vcc.template.common.entity.BaseAuditEntity;
import com.vcc.template.project.entity.Project;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.List;

@Entity
@Table(name = "kong_routes")
@Getter
@Setter
public class KongRouteEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kong_id", nullable = false, unique = true, length = 36)
    private String kongId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kong_service_id", nullable = false)
    private KongServiceEntity kongService;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "name", length = 200)
    private String name;

    @Column(name = "display_name", length = 200)
    private String displayName;

    @Column(name = "paths", columnDefinition = "TEXT")
    private String paths;

    @Column(name = "methods", columnDefinition = "TEXT")
    private String methods;

    @Column(name = "protocols", columnDefinition = "TEXT")
    private String protocols;

    @Column(name = "strip_path")
    private Boolean stripPath;

    @Column(name = "preserve_host")
    private Boolean preserveHost;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "hosts", columnDefinition = "jsonb")
    private List<String> hosts;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "headers", columnDefinition = "jsonb")
    private java.util.Map<String, List<String>> headers;

    @Column(name = "https_redirect_status")
    private Integer httpsRedirectStatus;

    @Column(name = "regex_priority")
    private Integer regexPriority;

    @Column(name = "path_handling", length = 20)
    private String pathHandling;

    @Column(name = "request_buffering")
    private Boolean requestBuffering;

    @Column(name = "response_buffering")
    private Boolean responseBuffering;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "snis", columnDefinition = "jsonb")
    private List<String> snis;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sources", columnDefinition = "jsonb")
    private List<java.util.Map<String, Object>> sources;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "destinations", columnDefinition = "jsonb")
    private List<java.util.Map<String, Object>> destinations;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", columnDefinition = "jsonb")
    private List<String> tags;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", length = 20)
    private SyncStatus syncStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private RouteStatus status = RouteStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kong_instance_id", nullable = false)
    private KongInstanceEntity kongInstance;
}
