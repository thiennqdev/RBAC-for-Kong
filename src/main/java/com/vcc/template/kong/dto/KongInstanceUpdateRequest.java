package com.vcc.template.kong.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KongInstanceUpdateRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Admin URL is required")
    private String adminUrl;

    private String description;

    private String status;
}
