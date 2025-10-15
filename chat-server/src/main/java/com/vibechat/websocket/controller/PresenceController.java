package com.vibechat.websocket.controller;

import com.vibechat.service.room.RoomParticipantService;
import com.vibechat.utils.session.StomSessionUtil;
import com.vibechat.websocket.session.WebSocketSessionInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;

/**
 * Presence 상태 처리 전담 WebSocket 컨트롤러 (리팩토링)
 *
 * SRP: Presence 관련 WebSocket 메시지만 담당
 * - 타이핑 상태 관리
 * - 사용자 상태 조회
 * - Presence 통계
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class PresenceController {

    private final RoomParticipantService roomParticipantService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 타이핑 상태 업데이트
     */
    @MessageMapping("/rooms/{roomId}/typing")
    public void updateTypingStatus(@DestinationVariable Long roomId,
                                   @Payload Map<String, Object> payload,
                                   SimpMessageHeaderAccessor headerAccessor) {

        try {
            // 1. 세션에서 사용자 정보 추출
            WebSocketSessionInfo sessionInfo = StomSessionUtil.extractSessionInfo(headerAccessor);

            if (sessionInfo == null) {
                log.warn("[타이핑 상태 업데이트 실패] 인증되지 않은 사용자: sessionId={}",
                    headerAccessor.getSessionId());
                return;
            }

            // 2. 페이로드 검증
            Boolean isTyping = (Boolean) payload.get("typing");
            if (isTyping == null) {
                log.warn("[타이핑 상태 업데이트 실패] 잘못된 페이로드: userId={}, roomId={}",
                    sessionInfo.getUserId(), roomId);
                return;
            }

            log.debug("[타이핑 상태 업데이트] userId={}, roomId={}, typing={}",
                sessionInfo.getUserId(), roomId, isTyping);

            // 3. 타이핑 상태 처리 (Redis Streams를 통한 브로드캐스트)
            processTypingStatus(roomId, sessionInfo, isTyping);

        } catch (Exception e) {
            log.error("[타이핑 상태 업데이트 예외] roomId={}, sessionId={}",
                roomId, headerAccessor.getSessionId(), e);
        }
    }

    /**
     * 방의 현재 Presence 상태 조회 및 브로드캐스트
     * 
     * 프론트엔드 구독 타이밍 문제 해결을 위한 수동 요청 핸들러
     */
    @MessageMapping("/rooms/{roomId}/presence/status")
    public void getRoomPresenceStatus(@DestinationVariable Long roomId,
                                      SimpMessageHeaderAccessor headerAccessor) {

        try {
            // 1. 인증 확인
            WebSocketSessionInfo sessionInfo = StomSessionUtil.extractSessionInfo(headerAccessor);
            if (sessionInfo == null) {
                sendPresenceError(headerAccessor.getSessionId(), "AUTHENTICATION_REQUIRED",
                    "인증이 필요합니다");
                return;
            }

            log.info("[Presence 수동 요청] userId={}, roomId={}", 
                sessionInfo.getUserId(), roomId);

            // 2. 방별 참가자 수 및 목록 조회
            int participantCount = roomParticipantService.getParticipantCount(roomId);
            List<Long> participantIds = roomParticipantService.getParticipants(roomId);

            // 3. 방 전체 브로드캐스트 (모든 구독자가 받도록)
            // TODO: 참가자 상세 정보 (nickname, avatarUrl) 추가 필요
            Map<String, Object> presenceEvent = Map.of(
                "count", participantCount,
                "participantIds", participantIds,
                "timestamp", System.currentTimeMillis()
            );

            String destination = String.format("/topic/rooms/%d/presence", roomId);
            messagingTemplate.convertAndSend(destination, presenceEvent);

            log.info("[Presence 수동 브로드캐스트] roomId={}, count={}, destination={}", 
                roomId, participantCount, destination);

        } catch (Exception e) {
            log.error("[Presence 상태 조회 예외] roomId={}, sessionId={}",
                roomId, headerAccessor.getSessionId(), e);
            sendPresenceError(headerAccessor.getSessionId(), "INTERNAL_ERROR",
                "Presence 상태 조회 중 오류가 발생했습니다");
        }
    }


    /**
     * 타이핑 상태 처리
     */
    private void processTypingStatus(Long roomId, WebSocketSessionInfo sessionInfo, Boolean isTyping) {
        try {
            // TODO: Redis Streams를 통한 타이핑 이벤트 발행
            // 향후 TypingEventProducer 구현 예정

            log.info("[타이핑 상태 처리] userId={}, roomId={}, nickname={}, typing={}",
                sessionInfo.getUserId(), roomId, sessionInfo.getNickname(), isTyping);

            // 임시: 직접 브로드캐스트 (추후 Redis Streams로 대체)
            Map<String, Object> typingEvent = Map.of(
                "roomId", roomId,
                "userId", sessionInfo.getUserId(),
                "nickname", sessionInfo.getNickname(),
                "typing", isTyping,
                "timestamp", System.currentTimeMillis()
            );

            messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/typing", typingEvent);

        } catch (Exception e) {
            log.error("[타이핑 상태 처리 실패] userId={}, roomId={}",
                sessionInfo.getUserId(), roomId, e);
        }
    }

    /**
     * Presence 오류 응답 전송
     */
    private void sendPresenceError(String sessionId, String errorType, String errorMessage) {
        try {
            Map<String, Object> response = Map.of(
                "type", errorType,
                "message", errorMessage,
                "timestamp", System.currentTimeMillis()
            );

            messagingTemplate.convertAndSendToUser(sessionId, "/queue/presence/errors", response);
        } catch (Exception e) {
            log.error("[Presence 오류 응답 전송 실패] sessionId={}, errorType={}", sessionId, errorType, e);
        }
    }
}