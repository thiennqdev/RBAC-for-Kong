package com.vcc.template.common.exception;


import com.vcc.template.common.dto.ErrorResponse;
import com.vcc.template.common.exception.errorcode.BaseErrorCode;
import com.vcc.template.common.exception.errorcode.SystemErrorCode;
import com.vcc.template.common.util.MessageUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageUtil messageUtil;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected ErrorResponse handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ErrorResponse.FieldError> fieldErrors = new ArrayList<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            Object rejectedValue = ((FieldError) error).getRejectedValue();

            fieldErrors.add(ErrorResponse.FieldError.builder()
                    .field(fieldName)
                    .message(errorMessage)
                    .rejectedValue(rejectedValue)
                    .build());
        });

        BaseErrorCode errorCode = SystemErrorCode.VALIDATION_FAILED;
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(errorCode.getStatus().value())
                .error(errorCode.getCode())
                .message("Invalid input parameters")
                .path(request.getRequestURI())
                .fieldErrors(fieldErrors)
                .build();

        log.warn("Validation error: {} on path: {}", fieldErrors, request.getRequestURI());
        return errorResponse;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Failed to read request, reason: {}", ex.getMessage());
        BaseErrorCode errorCode = SystemErrorCode.HTTP_REQUEST_NOT_READABLE;
        return ErrorResponse.of(errorCode.getStatus().value(),
                errorCode.getCode(),
                ex.getMessage(),
                request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGenericException(
            Exception ex,
            HttpServletRequest request) {
        log.error("Unhandled exception on path: {}", request.getRequestURI(), ex);
        BaseErrorCode errorCode = SystemErrorCode.SYSTEM_ERROR;
        return ErrorResponse.of(
                errorCode.getStatus().value(),
                errorCode.getCode(),
                "An unexpected error occurred: " + ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleRuntimeException(
            RuntimeException ex,
            HttpServletRequest request) {
        log.error("Runtime exception on path: {}", request.getRequestURI(), ex);
        BaseErrorCode errorCode = SystemErrorCode.RUNTIME_ERROR;
        return ErrorResponse.of(
                errorCode.getStatus().value(),
                errorCode.getCode(),
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNoResourceFoundException(
            NoResourceFoundException ex,
            HttpServletRequest request) {
        log.warn(ex.getMessage());
        BaseErrorCode errorCode = SystemErrorCode.RESOURCE_NOT_FOUND;
        return ErrorResponse.of(
                errorCode.getStatus().value(),
                errorCode.getCode(),
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        BaseErrorCode errorCode = ex.getErrorCode();
        String message = messageUtil.getMessage(errorCode.getCode(), ex.getArgs());
        log.warn("Business exception: Code {} on path {}", errorCode.getCode(), request.getRequestURI());
        return new ResponseEntity<>(
                ErrorResponse.of(
                        errorCode.getStatus().value(),
                        errorCode.getCode(),
                        message,
                        request.getRequestURI()
                ), errorCode.getStatus()
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        log.warn("Authentication Exception: {}", ex.getMessage());
        BaseErrorCode errorCode = SystemErrorCode.UNAUTHORIZED;
        return ErrorResponse.of(
                errorCode.getStatus().value(),
                errorCode.getCode(),
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied exception: {}", ex.getMessage());
        BaseErrorCode errorCode = SystemErrorCode.PERMISSION_DENIED;
        return ErrorResponse.of(
                errorCode.getStatus().value(),
                errorCode.getCode(),
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public ErrorResponse handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        log.warn("Unsupported MediaType exception on path: {}, {}",request.getRequestURI(), ex.getMessage());
        BaseErrorCode errorCode = SystemErrorCode.UNSUPPORTED_MEDIA_TYPE;
        return ErrorResponse.of(
                errorCode.getStatus().value(),
                errorCode.getCode(),
                ex.getMessage(),
                request.getRequestURI()
        );
    }
}
