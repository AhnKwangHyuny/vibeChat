package com.vibechat.service.websocket;

import com.vibechat.service.chat.ChatResponseHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * WebSocket 예외 처리 전담 서비스
 *
 * Clean Architecture 원칙에 따라 WebSocket 관련 예외를 일관되게 처리:
 * - 인증 실패 예외 처리
 * - 비즈니스 로직 예외 처리
 * - 시스템 예외 처리
 * - 표준화된 에러 응답 생성
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketExceptionHandler {

    private final ChatResponseHandler chatResponseHandler;

    /**
     * 인증 실패 예외 처리
     */
    public void handleAuthenticationError(String sessionId, String clientTempId, String operation) {
        log.warn("[WebSocket 인증 실패] operation={}, sessionId={}, clientTempId={}",
            operation, sessionId, clientTempId);

        chatResponseHandler.sendErrorResponse(sessionId, clientTempId, "AUTHENTICATION_REQUIRED",
            "인증된 사용자만 " + operation + "을 수행할 수 있습니다");
    }

    /**
     * 방 입장 예외 처리
     */
    public void handleRoomJoinError(String sessionId, Long roomId, Exception e) {
        log.error("[방 입장 실패] roomId={}, sessionId={}", roomId, sessionId, e);

        chatResponseHandler.sendErrorResponse(sessionId, "", "ROOM_JOIN_FAILED",
            "방 입장에 실패했습니다: " + e.getMessage());
    }

    /**
     * 방 퇴장 예외 처리
     */
    public void handleRoomLeaveError(String sessionId, Long roomId, Exception e) {
        log.error("[방 퇴장 실패] roomId={}, sessionId={}", roomId, sessionId, e);

        chatResponseHandler.sendErrorResponse(sessionId, "", "ROOM_LEAVE_FAILED",
            "방 퇴장에 실패했습니다: " + e.getMessage());
    }

    /**
     * 메시지 전송 예외 처리
     */
    public void handleMessageSendError(String sessionId, String clientTempId, Long roomId, Exception e) {
        log.error("[메시지 전송 실패] roomId={}, clientTempId={}, sessionId={}",
            roomId, clientTempId, sessionId, e);

        chatResponseHandler.sendErrorResponse(sessionId, clientTempId, "INTERNAL_ERROR",
            "메시지 처리 중 오류가 발생했습니다: " + e.getMessage());
    }

    /**
     * 일반적인 WebSocket 요청 예외 처리
     */
    public void handleGenericError(String sessionId, String clientTempId, String operation, Exception e) {
        log.error("[WebSocket 요청 실패] operation={}, sessionId={}, clientTempId={}",
            operation, sessionId, clientTempId, e);

        chatResponseHandler.sendErrorResponse(sessionId, clientTempId, "REQUEST_FAILED",
            operation + " 처리 중 오류가 발생했습니다: " + e.getMessage());
    }
}