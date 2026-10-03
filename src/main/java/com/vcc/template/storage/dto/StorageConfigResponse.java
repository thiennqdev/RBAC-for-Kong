package com.vcc.template.storage.dto;

import com.vcc.template.storage.entity.StorageStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StorageConfigResponse {
    private String projectCode;
    private String bucketName;
    private String accessKey;
    private String secretKey;
    private Long quotaInBytes;
    private boolean isPublic;
    private StorageStatus status;
}
