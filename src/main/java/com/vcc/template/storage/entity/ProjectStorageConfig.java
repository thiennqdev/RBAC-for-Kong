package com.vcc.template.storage.entity;

import com.vcc.template.common.entity.BaseAuditEntity;
import com.vcc.template.project.entity.Project;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(schema = "public", name = "project_storage_configs", uniqueConstraints = {
    @UniqueConstraint(name = "uk_storage_project_id", columnNames = "project_id"),
    @UniqueConstraint(name = "uk_storage_bucket_name", columnNames = "bucket_name")
})
@Getter
@Setter
public class ProjectStorageConfig extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "bucket_name", nullable = false, length = 100)
    private String bucketName;

    @Column(name = "access_key", nullable = false, length = 100)
    private String accessKey;

    @Column(name = "secret_key", nullable = false, length = 200)
    private String secretKey;

    @Column(name = "quota_in_bytes", nullable = false)
    private Long quotaInBytes;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StorageStatus status = StorageStatus.ACTIVE;
}
