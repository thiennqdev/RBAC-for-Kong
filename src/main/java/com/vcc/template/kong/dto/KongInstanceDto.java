package com.vcc.template.kong.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class KongInstanceDto {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String adminUrl;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
