package com.vcc.template.access.errorcode;

import com.vcc.template.common.exception.errorcode.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Error codes for project member access management.
 */
@Getter
@RequiredArgsConstructor
public enum AccessErrorCode implements BaseErrorCode {

    MEMBER_ALREADY_ASSIGNED("ACCESS-001", HttpStatus.CONFLICT),
    MEMBER_NOT_FOUND("ACCESS-002", HttpStatus.NOT_FOUND),
    CANNOT_REMOVE_LAST_ADMIN("ACCESS-003", HttpStatus.BAD_REQUEST),
    INVALID_ASSIGNMENT("ACCESS-004", HttpStatus.BAD_REQUEST),
    CANNOT_REMOVE_YOURSELF("ACCESS-005", HttpStatus.BAD_REQUEST),
    ROLE_NOT_FOUND("ACCESS-006", HttpStatus.NOT_FOUND);

    private final String code;
    private final HttpStatus status;
}
