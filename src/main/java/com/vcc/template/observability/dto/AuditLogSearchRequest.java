package com.vcc.template.observability.dto;

import com.vcc.template.common.dto.BaseSearchRequest;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class AuditLogSearchRequest extends BaseSearchRequest {
    private String action;
    private String entityName;
    private String actor;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
}
