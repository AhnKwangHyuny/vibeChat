package com.vibechat.config;

import com.vibechat.service.broadcast.WebSocketBroadcastServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * WebSocket 메시징 관련 순환 의존성 해결을 위한 설정
 */
@Configuration
public class WebSocketMessagingConfig {

    /**
     * Lazy 초기화로 순환 의존성 해결
     */
    @Bean
    public WebSocketBroadcastServiceImpl webSocketBroadcastServiceImpl(@Lazy SimpMessagingTemplate messagingTemplate) {
        WebSocketBroadcastServiceImpl service = new WebSocketBroadcastServiceImpl();
        service.setMessagingTemplate(messagingTemplate);
        return service;
    }
}