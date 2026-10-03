package com.vcc.template.observability.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/kong-logs")
@RequiredArgsConstructor
@Slf4j
public class KongInternalLogController {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${gateway.internal.secret:}")
    private String gatewayInternalSecret;

    @PostMapping
    public ResponseEntity<Void> receiveKongLog(@RequestBody String logPayload, HttpServletRequest request) {
        if (StringUtils.hasText(gatewayInternalSecret)) {
            String incomingKey = request.getHeader("X-Gateway-Internal-Key");
            if (!gatewayInternalSecret.equals(incomingKey)) {
                log.warn("Unauthorized internal log submission attempt from IP: {}", request.getRemoteAddr());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }

        log.debug("Received log from Kong http-log plugin: {}", 
                logPayload.substring(0, Math.min(logPayload.length(), 100)));
        kafkaTemplate.send("kong-log", logPayload);
        return ResponseEntity.ok().build();
    }
}
