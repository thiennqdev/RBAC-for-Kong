package com.vcc.template.observability.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time log streaming.
 * Uses STOMP protocol over WebSocket for pub/sub messaging.
 *
 * <p>Clients connect to /ws/logs and subscribe to /topic/logs.{projectCode}
 * to receive live API gateway logs for their specific project.</p>
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple in-memory message broker for /topic destinations
        config.enableSimpleBroker("/topic");
        // Set prefix for messages from client -> server
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket endpoint for log streaming
        registry.addEndpoint("/ws/logs")
                .setAllowedOriginPatterns("*");
    }
}
