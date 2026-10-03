package com.vcc.template.kong.dto;

import lombok.Data;
import java.util.Map;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class KongPluginRequest {
    @NotBlank
    private String pluginName;
    @NotBlank
    private String scope; // SERVICE, ROUTE
    @NotNull
    private Long scopeEntityId;
    private Map<String, Object> config;
    private Boolean enabled = true;
}
