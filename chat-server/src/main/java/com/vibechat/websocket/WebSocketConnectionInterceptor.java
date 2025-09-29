package com.vibechat.websocket;

import com.vibechat.websocket.auth.WebSocketAuthenticationException;
import com.vibechat.websocket.auth.WebSocketAuthenticationService;
import com.vibechat.websocket.connection.WebSocketConnectionEventService;
import com.vibechat.websocket.session.WebSocketSessionInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * WebSocket 연결 인터셉터 (완전 리팩토링)
 *
 * 책임: STOMP 명령어 인터셉트 및 적절한 서비스로 위임
 * SRP: 메시지 인터셉트만 담당, 실제 처리는 전문 서비스에 위임
 * OCP: 새로운 STOMP 명령어 처리 확장 가능
 * DIP: 구체 구현이 아닌 서비스 인터페이스에 의존
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketConnectionInterceptor implements ChannelInterceptor {

    private final WebSocketAuthenticationService authenticationService;
    private final WebSocketConnectionEventService connectionEventService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null) {
            StompCommand command = accessor.getCommand();

            if (command != null) {
                switch (command) {
                    case CONNECT:
                        return handleConnect(accessor, message);
                    case DISCONNECT:
                        return handleDisconnect(accessor, message);
                    default:
                        return message;
                }
            }
        }

        return message;
    }

    /**
     * CONNECT 명령어 처리
     */
    private Message<?> handleConnect(StompHeaderAccessor accessor, Message<?> message) {
        try {
            log.info("[WebSocket CONNECT 시작] sessionId={}", accessor.getSessionId());

            // 1. 사용자 인증
            WebSocketSessionInfo sessionInfo = authenticationService.authenticate(accessor);

            // 2. 세션에 인증 정보 저장
            authenticationService.storeSessionInfo(accessor, sessionInfo);

            // 3. 연결 이벤트 처리
            connectionEventService.handleUserConnected(sessionInfo);

            log.info("[WebSocket CONNECT 성공] userId={}, nickname={}, sessionId={}",
                sessionInfo.getUserId(), sessionInfo.getNickname(), sessionInfo.getSessionId());

            return message;

        } catch (WebSocketAuthenticationException e) {
            log.warn("[WebSocket CONNECT 인증 실패] sessionId={}, error={}",
                accessor.getSessionId(), e.getMessage());

            // 인증 실패 시 연결 거부
            throw new IllegalArgumentException(
                String.format("WebSocket 연결이 거부되었습니다: %s [%s]",
                    e.getMessage(), e.getErrorCode().getCode())
            );

        } catch (Exception e) {
            log.error("[WebSocket CONNECT 처리 실패] sessionId={}", accessor.getSessionId(), e);
            throw new IllegalStateException("WebSocket 연결 처리 중 오류가 발생했습니다", e);
        }
    }

    /**
     * DISCONNECT 명령어 처리
     */
    private Message<?> handleDisconnect(StompHeaderAccessor accessor, Message<?> message) {
        String sessionId = accessor.getSessionId();

        try {
            log.info("[WebSocket DISCONNECT 시작] sessionId={}", sessionId);

            // 연결 해제 이벤트 처리
            connectionEventService.handleUserDisconnected(sessionId);

            log.info("[WebSocket DISCONNECT 완료] sessionId={}", sessionId);

        } catch (Exception e) {
            log.error("[WebSocket DISCONNECT 처리 실패] sessionId={}", sessionId, e);
            // DISCONNECT 처리 실패가 연결 해제를 방해하지 않도록 예외를 던지지 않음
        }

        return message;
    }

    /**
     * 메시지 전송 후 후처리
     */
    @Override
    public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
        if (!sent) {
            StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
            if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                log.warn("[WebSocket CONNECT 메시지 전송 실패] sessionId={}", accessor.getSessionId());
            }
        }
    }

    /**
     * 연결 상태 모니터링을 위한 통계 조회
     */
    public Object getConnectionStats() {
        try {
            return connectionEventService.getConnectionStats();
        } catch (Exception e) {
            log.warn("[연결 통계 조회 실패]", e);
            return "통계 조회 불가";
        }
    }
}