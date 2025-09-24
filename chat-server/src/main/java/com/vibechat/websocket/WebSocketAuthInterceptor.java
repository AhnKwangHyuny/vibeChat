package com.vibechat.websocket;

import com.vibechat.service.presence.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * WebSocket 연결 시 사용자 정보를 세션에 저장
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final PresenceService presenceService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // CONNECT 시 헤더에서 사용자 정보 추출
            String userId = accessor.getFirstNativeHeader("userId");
            String nickname = accessor.getFirstNativeHeader("nickname");
            String avatarUrl = accessor.getFirstNativeHeader("avatarUrl");

            log.info("WebSocket CONNECT - userId: {}, nickname: {}", userId, nickname);

            if (userId != null && nickname != null) {
                // 세션 속성에 사용자 정보 저장
                Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                if (sessionAttributes != null) {
                    sessionAttributes.put("userId", Long.parseLong(userId));
                    sessionAttributes.put("nickname", nickname);
                    sessionAttributes.put("avatarUrl", avatarUrl);

                    log.info("User info stored in WebSocket session - userId: {}, nickname: {}", userId, nickname);
                }
            } else {
                log.warn("WebSocket CONNECT without valid user info - userId: {}, nickname: {}", userId, nickname);
                // 사용자 정보가 없으면 연결 거부할 수도 있음
                // throw new IllegalArgumentException("User authentication required");
            }
        }

        // DISCONNECT 처리
        if (accessor != null && StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            String sessionId = accessor.getSessionId();
            if (sessionId != null) {
                log.info("WebSocket DISCONNECT - sessionId: {}", sessionId);
                presenceService.userDisconnected(sessionId);
            }
        }
        return message;
    }
}