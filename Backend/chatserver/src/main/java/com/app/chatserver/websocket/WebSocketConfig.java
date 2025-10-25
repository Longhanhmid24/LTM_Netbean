package com.app.chatserver.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint để frontend kết nối (VD: ws://localhost:8080/ws)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // Cho phép mọi domain (nên giới hạn khi deploy)
                .withSockJS(); // Dự phòng nếu browser không hỗ trợ WebSocket
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Prefix cho server nhận tin nhắn từ client
        registry.setApplicationDestinationPrefixes("/app");
        // Prefix cho server gửi tin nhắn về client
        registry.enableSimpleBroker("/topic", "/queue");
    }
}
