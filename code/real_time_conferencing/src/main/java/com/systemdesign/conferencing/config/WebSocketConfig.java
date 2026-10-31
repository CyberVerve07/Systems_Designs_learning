package com.systemdesign.conferencing.config;

import com.systemdesign.conferencing.handler.SignalingWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

/**
 * WebSocket Endpoint Configuration for WebRTC & Mediasoup Signaling.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final SignalingWebSocketHandler signalingWebSocketHandler;

    public WebSocketConfig(SignalingWebSocketHandler signalingWebSocketHandler) {
        this.signalingWebSocketHandler = signalingWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(signalingWebSocketHandler, "/ws/signaling")
                .setAllowedOrigins("*"); // Allow all origins for local and cross-origin browser testing
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        // Configure buffer sizes for large SDP / ICE candidate payloads
        container.setMaxTextMessageBufferSize(64 * 1024); // 64 KB
        container.setMaxBinaryMessageBufferSize(64 * 1024); // 64 KB
        container.setMaxSessionIdleTimeout(300000L); // 5 minutes idle timeout
        return container;
    }
}

