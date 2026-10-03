package com.vcc.template.project.errorcode;

import com.vcc.template.common.exception.errorcode.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Error codes for project management operations.
 * Each code maps to localized messages in resource bundles.
 */
@Getter
@RequiredArgsConstructor
public enum ProjectErrorCode implements BaseErrorCode {

    /**
     * PROJECT-001: Attempted to create a project with a code that already exists.
     */
    PROJECT_ALREADY_EXISTS("PROJECT-001", HttpStatus.CONFLICT),

    /**
     * PROJECT-002: Requested project code does not exist.
     */
    PROJECT_NOT_FOUND("PROJECT-002", HttpStatus.NOT_FOUND),

    /**
     * PROJECT-003: Invalid icon file format (not PNG, JPEG, or SVG).
     */
    INVALID_ICON_FORMAT("PROJECT-003", HttpStatus.BAD_REQUEST),

    /**
     * PROJECT-004: Failed to create database schema for the project.
     */
    SCHEMA_CREATION_FAILED("PROJECT-004", HttpStatus.INTERNAL_SERVER_ERROR),

    /**
     * PROJECT-005: Project is inactive and cannot be accessed.
     */
    PROJECT_INACTIVE("PROJECT-005", HttpStatus.BAD_REQUEST),

    /**
     * PROJECT-006: Access denied, SYSTEM_ADMIN role required.
     */
    ACCESS_DENIED("PROJECT-006", HttpStatus.FORBIDDEN),

    /**
     * PROJECT-007: Invalid project code format.
     */
    INVALID_PROJECT_CODE("PROJECT-007", HttpStatus.BAD_REQUEST),

    /**
     * PROJECT-008: Failed to upload icon to storage.
     */
    ICON_UPLOAD_FAILED("PROJECT-008", HttpStatus.INTERNAL_SERVER_ERROR),

    /**
     * PROJECT-009: Icon path exceeds maximum length.
     */
    INVALID_ICON_PATH("PROJECT-009", HttpStatus.BAD_REQUEST),

    /**
     * PROJECT-010: File not found at specified path.
     */
    FILE_NOT_FOUND("PROJECT-010", HttpStatus.NOT_FOUND),

    /**
     * PROJECT-011: Failed to drop database schema during hard delete.
     */
    SCHEMA_DROP_FAILED("PROJECT-011", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final HttpStatus status;
}
