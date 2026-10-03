package com.vcc.template.storage.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class EnableStorageRequest {
    
    @NotNull(message = "Quota cannot be null")
    @Min(value = 1, message = "Minimum quota is 1")
    private Double quota;

    @NotNull(message = "Quota unit cannot be null")
    @Pattern(regexp = "^(MB|GB)$", message = "Quota unit must be MB or GB")
    private String quotaUnit;
    
    @NotNull(message = "Public flag cannot be null")
    private Boolean isPublic;
}
