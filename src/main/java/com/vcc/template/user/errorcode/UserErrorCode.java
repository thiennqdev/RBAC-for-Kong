package com.vcc.template.user.errorcode;

import com.vcc.template.common.exception.errorcode.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Error codes for user management operations.
 */
@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements BaseErrorCode {

    USER_NOT_FOUND("USER-001", HttpStatus.NOT_FOUND),
    USERNAME_ALREADY_EXISTS("USER-002", HttpStatus.CONFLICT),
    EMAIL_ALREADY_EXISTS("USER-003", HttpStatus.CONFLICT),
    USER_ALREADY_LOCKED("USER-004", HttpStatus.BAD_REQUEST),
    USER_ALREADY_ACTIVE("USER-005", HttpStatus.BAD_REQUEST),
    USER_CREATION_FAILED("USER-006", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final HttpStatus status;
}
