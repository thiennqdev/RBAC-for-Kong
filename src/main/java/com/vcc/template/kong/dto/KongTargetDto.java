package com.vcc.template.kong.dto;

import com.vcc.template.kong.entity.SyncStatus;
import lombok.Data;

@Data
public class KongTargetDto {
    private Long id;
    private String kongId;
    private Long kongUpstreamId;
    private String target;
    private Integer weight;
    private SyncStatus syncStatus;
    private String health;
    private java.util.List<String> tags;
}
