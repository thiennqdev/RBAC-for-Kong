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
@Table(name = "kong_services")
@Getter
@Setter
public class KongServiceEntity extends BaseAuditEntity {

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

    @Column(name = "host", length = 500)
    private String host;

    @Column(name = "port")
    private Integer port;

    @Column(name = "protocol", length = 10)
    private String protocol;

    @Column(name = "path", length = 500)
    private String path;

    @Column(name = "enabled")
    private Boolean enabled;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", length = 20)
    private SyncStatus syncStatus;

    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags;

    @Column(name = "retries")
    private Integer retries;

    @Column(name = "connect_timeout")
    private Integer connectTimeout;

    @Column(name = "write_timeout")
    private Integer writeTimeout;

    @Column(name = "read_timeout")
    private Integer readTimeout;

    @Column(name = "client_certificate", length = 36)
    private String clientCertificate;

    @Column(name = "tls_verify")
    private Boolean tlsVerify;

    @Column(name = "tls_verify_depth")
    private Integer tlsVerifyDepth;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ca_certificates", columnDefinition = "jsonb")
    private List<String> caCertificates;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kong_instance_id", nullable = false)
    private KongInstanceEntity kongInstance;
}
