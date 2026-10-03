package com.vcc.template.kong.entity;

import com.vcc.template.common.entity.BaseAuditEntity;
import com.vcc.template.project.entity.Project;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "kong_targets")
@Getter
@Setter
public class KongTargetEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kong_id", nullable = false, unique = true, length = 36)
    private String kongId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kong_upstream_id", nullable = false)
    private KongUpstreamEntity kongUpstream;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "target", nullable = false, length = 500)
    private String target;

    @Column(name = "weight")
    private Integer weight = 100;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", length = 20)
    private SyncStatus syncStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", columnDefinition = "jsonb")
    private java.util.List<String> tags;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kong_instance_id", nullable = false)
    private KongInstanceEntity kongInstance;
}
