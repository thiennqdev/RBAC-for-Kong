package com.vcc.template.common.exception.errorcode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SystemErrorCode implements BaseErrorCode {
    SYSTEM_ERROR("SYS-001", HttpStatus.INTERNAL_SERVER_ERROR),
    RUNTIME_ERROR("SYS-002", HttpStatus.INTERNAL_SERVER_ERROR),
    HTTP_REQUEST_NOT_READABLE("SYS-003", HttpStatus.BAD_REQUEST),
    VALIDATION_FAILED("SYS-004", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("SYS-005", HttpStatus.UNAUTHORIZED),
    PERMISSION_DENIED("SYS-006", HttpStatus.FORBIDDEN),
    RESOURCE_NOT_FOUND("SYS-007", HttpStatus.NOT_FOUND),
    UNSUPPORTED_MEDIA_TYPE("SYS-008", HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    private final String code;
    private final HttpStatus status;
}
