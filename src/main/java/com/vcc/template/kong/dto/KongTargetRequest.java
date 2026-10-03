package com.vcc.template.kong.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class KongTargetRequest {

    @NotBlank(message = "Target address is required (e.g., 192.168.1.1:8080)")
    private String target;

    @Min(value = 0, message = "Weight must be between 0 and 65535")
    @Max(value = 65535, message = "Weight must be between 0 and 65535")
    private Integer weight = 100;

    private java.util.List<String> tags;
}
