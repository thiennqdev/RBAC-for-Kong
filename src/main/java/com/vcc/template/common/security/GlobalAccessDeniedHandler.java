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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GlobalAccessDeniedHandler implements AccessDeniedHandler {

    private final ResponseUtil responseUtil;

    @Override
    public void handle(HttpServletRequest request, @NonNull HttpServletResponse response, AccessDeniedException accessDeniedException) {
        log.warn("Access Denied exception: {}", accessDeniedException.getMessage());
        BaseErrorCode errorCode = SystemErrorCode.PERMISSION_DENIED;
        ErrorResponse errorResponse = ErrorResponse.of(errorCode.getStatus().value(), errorCode.getCode(),
                accessDeniedException.getMessage(), request.getRequestURI());
        responseUtil.sendServletErrorResponse(response, errorResponse);
    }
}
