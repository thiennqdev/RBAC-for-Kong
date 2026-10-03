package com.vcc.template.kong.entity;

import com.vcc.template.common.entity.BaseAuditEntity;
import com.vcc.template.project.entity.Project;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "kong_upstreams")
@Getter
@Setter
public class KongUpstreamEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kong_id", nullable = false, unique = true, length = 36)
    private String kongId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "display_name", length = 200)
    private String displayName;

    @Column(name = "algorithm", length = 50)
    private String algorithm;

    @Column(name = "slots")
    private Integer slots;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", length = 20)
    private SyncStatus syncStatus;

    @Column(name = "healthchecks_config", columnDefinition = "TEXT")
    private String healthchecksConfig;

    @Column(name = "hash_on", length = 50)
    private String hashOn;

    @Column(name = "hash_fallback", length = 50)
    private String hashFallback;

    @Column(name = "hash_on_header", length = 100)
    private String hashOnHeader;

    @Column(name = "hash_fallback_header", length = 100)
    private String hashFallbackHeader;

    @Column(name = "hash_on_cookie", length = 100)
    private String hashOnCookie;

    @Column(name = "hash_on_cookie_path", length = 200)
    private String hashOnCookiePath;

    @Column(name = "hash_on_query_arg", length = 100)
    private String hashOnQueryArg;

    @Column(name = "hash_fallback_query_arg", length = 100)
    private String hashFallbackQueryArg;

    @Column(name = "hash_on_uri_capture", length = 100)
    private String hashOnUriCapture;

    @Column(name = "hash_fallback_uri_capture", length = 100)
    private String hashFallbackUriCapture;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "healthchecks", columnDefinition = "jsonb")
    private java.util.Map<String, Object> healthchecks;

    @Column(name = "host_header", length = 200)
    private String hostHeader;

    @Column(name = "client_certificate", length = 36)
    private String clientCertificate;

    @Column(name = "use_srv_name")
    private Boolean useSrvName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", columnDefinition = "jsonb")
    private java.util.List<String> tags;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kong_instance_id", nullable = false)
    private KongInstanceEntity kongInstance;
}
