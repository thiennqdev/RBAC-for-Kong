package com.vcc.template.common.util;

import com.vcc.template.common.dto.ErrorResponse;
import com.vcc.template.common.dto.PaginationResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
@Slf4j
@Component
public class ResponseUtil {
    private final ObjectMapper objectMapper;

    public void sendServletErrorResponse(HttpServletResponse response, ErrorResponse errorResponse) {
        try {
            String body = objectMapper.writeValueAsString(errorResponse);
            PrintWriter printWriter = response.getWriter();
            response.setStatus(errorResponse.getStatus());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            printWriter.print(body);
            printWriter.flush();
        } catch (Exception e) {
            log.error("ERROR", e);
        }
    }


    public static <T> PaginationResponse<T> toPagingResponse(@NonNull Page<T> page) {
        return PaginationResponse.<T>builder()
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .data(page.getContent())
                .build();
    }
}
