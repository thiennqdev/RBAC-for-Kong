package com.vcc.template.common.file.errorcode;

import com.vcc.template.common.exception.errorcode.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Error codes for file storage operations.
 */
@Getter
@RequiredArgsConstructor
public enum FileErrorCode implements BaseErrorCode {
    INVALID_FILE_TYPE("FILE-001", HttpStatus.BAD_REQUEST),
    FILE_TOO_LARGE("FILE-002", HttpStatus.BAD_REQUEST),
    FILE_UPLOAD_FAILED("FILE-003", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_CATEGORY("FILE-004", HttpStatus.BAD_REQUEST);
    
    private final String code;
    private final HttpStatus status;
}
