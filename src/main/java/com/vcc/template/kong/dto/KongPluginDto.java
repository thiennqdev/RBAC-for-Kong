package com.vcc.template.kong.dto;

import com.vcc.template.kong.entity.SyncStatus;
import lombok.Data;
import java.util.Map;

@Data
public class KongPluginDto {
    private Long id;
    private String kongId;
    private String pluginName;
    private String scope;
    private Long scopeEntityId;
    private Map<String, Object> config;
    private Boolean enabled;
    private SyncStatus syncStatus;
}
