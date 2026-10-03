package com.vcc.template.storage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageUsageDto {
    private long usedBytes;
    private long quotaBytes;
    private int objectCount;
}
