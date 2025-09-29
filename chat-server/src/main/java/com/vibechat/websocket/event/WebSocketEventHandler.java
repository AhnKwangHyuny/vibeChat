package com.vibechat.websocket.event;

import com.vibechat.websocket.connection.WebSocketConnectionEventService;
import com.vibechat.websocket.presence.WebSocketPresenceService;
import com.vibechat.websocket.session.WebSocketSessionInfo;
import com.vibechat.service.room.RoomParticipantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.Map;

/**
 * WebSocket 이벤트 핸들러 (완전 리팩토링)
 *
 * SRP: WebSocket 생명주기 이벤트 처리만 담당
 * 기존 StompEventListener를 대체하여 새로운 아키텍처와 호환
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventHandler {

    private final WebSocketConnectionEventService connectionEventService;
    private final WebSocketPresenceService presenceService;
    private final RoomParticipantService roomParticipantService;

    /**
     * WebSocket 연결 완료 시 후처리
     *
     * Note: 실제 인증은 WebSocketConnectionInterceptor에서 이미 완료됨
     */
    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        try {
            log.info("[WebSocket 연결 완료 이벤트] sessionId={}", sessionId);

            // 연결 완료 통계 업데이트 등의 후처리
            updateConnectionStatistics();

            log.debug("[WebSocket 연결 이벤트 처리 완료] sessionId={}", sessionId);

        } catch (Exception e) {
            log.warn("[WebSocket 연결 이벤트 처리 실패] sessionId={}", sessionId, e);
            // 연결 완료 후 처리 실패가 연결 자체에 영향을 주지 않도록 예외를 던지지 않음
        }
    }

    /**
     * WebSocket 연결 해제 시 후처리
     *
     * Note: SessionDisconnectEvent는 모든 연결 해제에 대해 발생 (정상/비정상 모두)
     */
    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        try {
            log.info("[WebSocket 연결 해제 이벤트] sessionId={}", sessionId);

            // WebSocketConnectionInterceptor에서 이미 처리되었지만,
            // 예외적인 상황(네트워크 끊김 등)에 대한 안전장치로 재처리
            connectionEventService.handleUserDisconnected(sessionId);

            log.debug("[WebSocket 연결 해제 이벤트 처리 완료] sessionId={}", sessionId);

        } catch (Exception e) {
            log.warn("[WebSocket 연결 해제 이벤트 처리 실패] sessionId={}", sessionId, e);
            // 연결 해제 후 처리 실패가 정리 과정에 영향을 주지 않도록 예외를 던지지 않음
        }
    }

    /**
     * 구독 이벤트 처리
     *
     * 방 구독 시 추가 처리 (Analytics, 통계 등)
     */
    @EventListener
    public void handleSessionSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        String sessionId = accessor.getSessionId();

        try {
            log.info("[WebSocket 구독 이벤트] sessionId={}, destination={}", sessionId, destination);

            if (destination != null && destination.matches("/topic/rooms/\\d+/messages")) {
                // 방 메시지 구독 시 처리
                handleRoomSubscription(accessor, destination);
            } else if (destination != null && destination.matches("/topic/rooms/\\d+/presence")) {
                // 방 Presence 구독 시 처리
                handlePresenceSubscription(accessor, destination);
            }

        } catch (Exception e) {
            log.warn("[WebSocket 구독 이벤트 처리 실패] sessionId={}, destination={}",
                sessionId, destination, e);
        }
    }

    /**
     * 방 메시지 구독 처리 (방 입장 로직 포함)
     */
    private void handleRoomSubscription(StompHeaderAccessor accessor, String destination) {
        try {
            // destination에서 roomId 추출: /topic/rooms/{roomId}/messages
            String[] parts = destination.split("/");
            Long roomId = Long.parseLong(parts[3]);
            String sessionId = accessor.getSessionId();

            // 세션에서 사용자 정보 추출
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes != null) {
                WebSocketSessionInfo sessionInfo = (WebSocketSessionInfo) sessionAttributes.get("sessionInfo");

                if (sessionInfo != null) {
                    Long userId = sessionInfo.getUserId();

                    log.info("[방 메시지 구독 - 방 입장 처리] userId={}, roomId={}, sessionId={}",
                        userId, roomId, sessionId);

                    // ** 핵심: 방 입장 로직 **
                    handleRoomEntry(roomId, userId, sessionInfo);

                    // 방 구독 통계 업데이트
                    updateRoomSubscriptionStats(roomId, userId);
                }
            }

        } catch (Exception e) {
            log.warn("[방 메시지 구독 처리 실패] destination={}", destination, e);
        }
    }

    /**
     * Presence 구독 처리
     */
    private void handlePresenceSubscription(StompHeaderAccessor accessor, String destination) {
        try {
            // destination에서 roomId 추출: /topic/rooms/{roomId}/presence
            String[] parts = destination.split("/");
            Long roomId = Long.parseLong(parts[3]);

            log.debug("[Presence 구독] roomId={}, sessionId={}", roomId, accessor.getSessionId());

            // 현재 방의 Presence 상태를 구독자에게 즉시 전송 (선택적)
            // sendCurrentPresenceState(roomId, accessor.getSessionId());

        } catch (Exception e) {
            log.warn("[Presence 구독 처리 실패] destination={}", destination, e);
        }
    }

    /**
     * 연결 통계 업데이트
     */
    private void updateConnectionStatistics() {
        try {
            // 연결 통계 업데이트 로직
            // 예: 메트릭 수집, 모니터링 시스템에 전송 등
            var stats = connectionEventService.getConnectionStats();
            log.debug("[연결 통계] {}", stats);
        } catch (Exception e) {
            log.debug("[연결 통계 업데이트 실패]", e);
        }
    }

    /**
     * 방 입장 핵심 로직 (비즈니스 로직)
     *
     * 방 구독 시 자동으로 방 참가자로 등록
     */
    private void handleRoomEntry(Long roomId, Long userId, WebSocketSessionInfo sessionInfo) {
        try {
            // 1. 이미 참가자인지 확인
            boolean isAlreadyParticipant = roomParticipantService.isParticipant(roomId, userId);

            if (!isAlreadyParticipant) {
                // 2. 방 참가자로 등록
                roomParticipantService.addParticipant(roomId, userId);

                log.info("[방 입장 완료] userId={}, nickname={}, roomId={} - 새로운 참가자 등록",
                    userId, sessionInfo.getNickname(), roomId);

                // 3. 방 입장 시스템 메시지 전송 (선택적)
                sendRoomJoinSystemMessage(roomId, sessionInfo);

                // 4. 방 통계 정보 업데이트
                logRoomParticipantStats(roomId);

            } else {
                log.debug("[방 재입장] userId={}, nickname={}, roomId={} - 기존 참가자",
                    userId, sessionInfo.getNickname(), roomId);
            }

        } catch (Exception e) {
            log.error("[방 입장 처리 실패] userId={}, roomId={}", userId, roomId, e);
            // 방 입장 실패가 구독 자체를 방해하지 않도록 예외를 다시 던지지 않음
        }
    }

    /**
     * 방 입장 시스템 메시지 전송 (선택적 구현)
     */
    private void sendRoomJoinSystemMessage(Long roomId, WebSocketSessionInfo sessionInfo) {
        try {
            // TODO: 시스템 메시지 전송 로직 구현
            // 예: "{nickname}님이 입장하셨습니다" 메시지를 방의 모든 참가자에게 전송

            log.debug("[시스템 메시지] {}님이 방 {}에 입장하셨습니다",
                sessionInfo.getNickname(), roomId);

        } catch (Exception e) {
            log.warn("[방 입장 시스템 메시지 전송 실패] roomId={}, userId={}",
                roomId, sessionInfo.getUserId(), e);
        }
    }

    /**
     * 방 참가자 통계 로깅
     */
    private void logRoomParticipantStats(Long roomId) {
        try {
            var stats = roomParticipantService.getStats(roomId);
            log.info("[방 참가자 통계] {}", stats.toString());
        } catch (Exception e) {
            log.debug("[방 참가자 통계 조회 실패] roomId={}", roomId, e);
        }
    }

    /**
     * 방 구독 통계 업데이트
     */
    private void updateRoomSubscriptionStats(Long roomId, Long userId) {
        try {
            // 방별 구독 통계 업데이트
            int participantCount = roomParticipantService.getParticipantCount(roomId);
            log.debug("[방 구독 통계] roomId={}, userId={}, totalParticipants={}",
                roomId, userId, participantCount);
        } catch (Exception e) {
            log.debug("[방 구독 통계 업데이트 실패] roomId={}", roomId, e);
        }
    }
}