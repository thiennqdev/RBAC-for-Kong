package com.vcc.template.common.security;

import com.vcc.template.common.dto.ErrorResponse;
import com.vcc.template.common.exception.errorcode.BaseErrorCode;
import com.vcc.template.common.exception.errorcode.SystemErrorCode;
import com.vcc.template.common.util.ResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GlobalAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ResponseUtil responseUtil;

    @Override
    public void commence(HttpServletRequest request, @NonNull HttpServletResponse response, AuthenticationException authException) {
        log.warn("Authentication Exception: {}", authException.getMessage());
        BaseErrorCode errorCode = SystemErrorCode.UNAUTHORIZED;
        ErrorResponse errorResponse = ErrorResponse.of(errorCode.getStatus().value(), errorCode.getCode(),
                authException.getMessage(), request.getRequestURI());
        responseUtil.sendServletErrorResponse(response, errorResponse);
    }
}
