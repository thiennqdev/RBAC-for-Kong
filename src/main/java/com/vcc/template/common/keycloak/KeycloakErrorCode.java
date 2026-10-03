package com.vcc.template.common.keycloak;

import com.vcc.template.common.exception.errorcode.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Error codes for Keycloak integration operations.
 */
@Getter
@RequiredArgsConstructor
public enum KeycloakErrorCode implements BaseErrorCode {

    KC_USER_CREATION_FAILED("KC-001", HttpStatus.INTERNAL_SERVER_ERROR),
    KC_USER_UPDATE_FAILED("KC-002", HttpStatus.INTERNAL_SERVER_ERROR),
    KC_SYNC_FAILED("KC-003", HttpStatus.INTERNAL_SERVER_ERROR),
    KC_CONNECTION_ERROR("KC-004", HttpStatus.SERVICE_UNAVAILABLE);

    private final String code;
    private final HttpStatus status;
}
