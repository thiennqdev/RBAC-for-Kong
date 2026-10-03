package com.vcc.template.storage.service;

import com.vcc.template.project.entity.Project;
import com.vcc.template.project.errorcode.ProjectErrorCode;
import com.vcc.template.project.repository.ProjectRepository;
import com.vcc.template.storage.dto.EnableStorageRequest;
import com.vcc.template.storage.dto.StorageConfigResponse;
import com.vcc.template.storage.entity.ProjectStorageConfig;
import com.vcc.template.storage.repository.ProjectStorageConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectStorageService {

    private final ProjectRepository projectRepository;
    private final ProjectStorageConfigRepository storageConfigRepository;
    private final StorageProvisioningService provisioningService;

    @Transactional
    public StorageConfigResponse enableStorageForProject(String projectCode, EnableStorageRequest request) {
        log.info("Enabling storage for project: {}", projectCode);

        // 1. Validate project exists
        Project project = projectRepository.findByProjectCode(projectCode)
                .orElseThrow(() -> ProjectErrorCode.PROJECT_NOT_FOUND.toException(projectCode));

        // 2. Check if already enabled
        if (storageConfigRepository.existsByProject_ProjectCode(projectCode)) {
            throw new RuntimeException("Storage is already enabled for project " + projectCode);
        }

        String bucketName = "bucket-" + projectCode.toLowerCase();

        long quotaInBytes = calculateQuotaInBytes(request.getQuota(), request.getQuotaUnit());

        ProjectStorageConfig config = new ProjectStorageConfig();
        config.setProject(project);
        config.setBucketName(bucketName);
        config.setQuotaInBytes(quotaInBytes);
        config.setPublic(request.getIsPublic());

        try {
            // 3. Call MinIO to create bucket
            provisioningService.createBucket(bucketName);
            
            // 4. Set quota
            provisioningService.setBucketQuota(bucketName, quotaInBytes);

            // 5. Apply policy
            provisioningService.applyBucketPolicy(bucketName, request.getIsPublic());

            // 6. Create Service Account (Keys)
            var credentials = provisioningService.createServiceAccountForBucket(bucketName);
            config.setAccessKey(credentials.accessKey());
            config.setSecretKey(credentials.secretKey());

        } catch (Exception e) {
            log.error("Failed to provision storage on MinIO for project {}", projectCode, e);
            throw new RuntimeException("Failed to provision storage on MinIO: " + e.getMessage(), e);
        }

        // 7. Save to DB
        ProjectStorageConfig savedConfig = storageConfigRepository.save(config);

        return toDto(savedConfig, true);
    }

    @Transactional(readOnly = true)
    public StorageConfigResponse getStorageConfig(String projectCode) {
        ProjectStorageConfig config = storageConfigRepository.findByProject_ProjectCode(projectCode)
                .orElseThrow(() -> new RuntimeException("Storage not enabled for project " + projectCode));

        return toDto(config, false);
    }

    @Transactional
    public StorageConfigResponse rotateCredentials(String projectCode) {
        log.info("Rotating storage credentials for project: {}", projectCode);
        ProjectStorageConfig config = storageConfigRepository.findByProject_ProjectCode(projectCode)
                .orElseThrow(() -> new RuntimeException("Storage not enabled for project " + projectCode));

        // 1. Delete old service account
        provisioningService.deleteServiceAccount(config.getAccessKey());

        try {
            // 2. Create new service account
            var credentials = provisioningService.createServiceAccountForBucket(config.getBucketName());
            
            // 3. Update DB
            config.setAccessKey(credentials.accessKey());
            config.setSecretKey(credentials.secretKey());
            
            ProjectStorageConfig savedConfig = storageConfigRepository.save(config);
            return toDto(savedConfig, true);
            
        } catch (Exception e) {
            log.error("Failed to rotate credentials on MinIO for project {}", projectCode, e);
            throw new RuntimeException("Failed to rotate credentials: " + e.getMessage(), e);
        }
    }

    public java.util.List<com.vcc.template.storage.dto.StoragePolicyRuleDto> getCustomPolicies(String projectCode) {
        ProjectStorageConfig config = storageConfigRepository.findByProject_ProjectCode(projectCode)
                .orElseThrow(() -> new RuntimeException("Storage not enabled for project " + projectCode));
        try {
            return provisioningService.getCustomPolicies(config.getBucketName());
        } catch (Exception e) {
            throw new RuntimeException("Failed to get bucket policies: " + e.getMessage(), e);
        }
    }

    public void setCustomPolicies(String projectCode, java.util.List<com.vcc.template.storage.dto.StoragePolicyRuleDto> rules) {
        ProjectStorageConfig config = storageConfigRepository.findByProject_ProjectCode(projectCode)
                .orElseThrow(() -> new RuntimeException("Storage not enabled for project " + projectCode));
        try {
            provisioningService.setCustomPolicies(config.getBucketName(), rules);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set bucket policies: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void deleteStorageForProject(Long projectId) {
        storageConfigRepository.findByProjectId(projectId).ifPresent(config -> {
            try {
                provisioningService.deleteStorage(config.getBucketName(), config.getAccessKey());
            } catch (Exception e) {
                log.warn("Failed to delete MinIO resources for project {}: {}", projectId, e.getMessage());
            }
            storageConfigRepository.delete(config);
        });
    }

    private StorageConfigResponse toDto(ProjectStorageConfig config, boolean includeSecret) {
        return StorageConfigResponse.builder()
                .projectCode(config.getProject().getProjectCode())
                .bucketName(config.getBucketName())
                .accessKey(config.getAccessKey())
                .secretKey(includeSecret ? config.getSecretKey() : null)
                .quotaInBytes(config.getQuotaInBytes())
                .isPublic(config.isPublic())
                .status(config.getStatus())
                .build();
    }

    private long calculateQuotaInBytes(Double quota, String unit) {
        if (quota == null || unit == null) {
            return 0;
        }
        long multiplier = 1;
        if ("MB".equalsIgnoreCase(unit)) {
            multiplier = 1024L * 1024L;
        } else if ("GB".equalsIgnoreCase(unit)) {
            multiplier = 1024L * 1024L * 1024L;
        }
        return (long) (quota * multiplier);
    }
}
