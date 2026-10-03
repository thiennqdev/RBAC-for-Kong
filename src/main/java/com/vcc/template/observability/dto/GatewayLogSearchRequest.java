package com.vcc.template.observability.dto;

import com.vcc.template.common.dto.BaseSearchRequest;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class GatewayLogSearchRequest extends BaseSearchRequest {
    private String method;
    private Integer statusCode;
    private String clientIp;
    private String uri;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
}
