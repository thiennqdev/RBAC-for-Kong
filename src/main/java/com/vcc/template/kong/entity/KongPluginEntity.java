package com.vcc.template.kong.entity;

import com.vcc.template.common.entity.BaseAuditEntity;
import com.vcc.template.project.entity.Project;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "kong_plugins")
@Getter
@Setter
public class KongPluginEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kong_id", nullable = false, unique = true, length = 36)
    private String kongId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "plugin_name", nullable = false, length = 100)
    private String pluginName;

    @Column(name = "scope", nullable = false, length = 20)
    private String scope;

    @Column(name = "scope_entity_id")
    private Long scopeEntityId;

    @Column(name = "config", columnDefinition = "TEXT")
    private String config;

    @Column(name = "enabled")
    private Boolean enabled;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", length = 20)
    private SyncStatus syncStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kong_instance_id", nullable = false)
    private KongInstanceEntity kongInstance;
}
