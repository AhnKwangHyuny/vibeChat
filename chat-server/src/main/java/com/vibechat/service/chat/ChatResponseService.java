package com.vibechat.service.chat;

import com.vibechat.service.chat.dto.RoomJoinResult;
import com.vibechat.service.chat.dto.RoomLeaveResult;
import com.vibechat.dto.coordinator.MessageProcessResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 채팅 응답 전송 구현체
 *
 * 구현 내용:
 * - SimpMessagingTemplate를 이용한 WebSocket 메시지 전송
 * - 방별 브로드캐스트 채널 관리 (/topic/rooms/{roomId}/messages)
 * - 개인별 응답 채널 관리 (/queue/ack, /queue/errors, /queue/room-*-response)
 * - 응답 메시지 JSON 포맷팅 및 타임스탬프 추가
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatResponseService implements ChatResponseHandler {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 메시지 전송 결과 응답 처리 구현
     */
    @Override
    public void handleMessageResult(String sessionId, MessageProcessResult result, String clientTempId) {
        if (result.isSuccess()) {
            sendSuccessResponse(sessionId, result);
            log.info("[메시지 전송 성공] messageId={}, clientTempId={}, processingTime={}ms",
                result.getMessageId(), result.getClientTempId(), result.getProcessingTimeMs());
        } else {
            sendErrorResponse(sessionId, clientTempId, "MESSAGE_PROCESSING_FAILED", result.getErrorMessage());
            log.warn("[메시지 전송 실패] error={}, clientTempId={}",
                result.getErrorMessage(), clientTempId);
        }
    }

    /**
     * 방 입장 결과 응답 처리 구현
     */
    @Override
    public void handleRoomJoinResult(String sessionId, RoomJoinResult result) {
        if (result.isSuccess()) {
            sendRoomJoinSuccessResponse(sessionId, result);

            // 시스템 메시지 브로드캐스트
            if (result.hasSystemMessage()) {
                broadcastSystemMessage(result.getRoomId(), "USER_JOINED", result);
            }

            log.info("[방 입장 완료] userId={}, roomId={}, nickname={}",
                result.getUserId(), result.getRoomId(), result.getNickname());
        } else {
            sendErrorResponse(sessionId, "", "ROOM_JOIN_FAILED", result.getErrorMessage());
            log.error("[방 입장 실패] userId={}, roomId={}, error={}",
                result.getUserId(), result.getRoomId(), result.getErrorMessage());
        }
    }

    /**
     * 방 퇴장 결과 응답 처리 구현
     */
    @Override
    public void handleRoomLeaveResult(String sessionId, RoomLeaveResult result) {
        if (result.isSuccess()) {
            sendRoomLeaveSuccessResponse(sessionId, result);

            // 시스템 메시지 브로드캐스트
            if (result.hasSystemMessage()) {
                broadcastSystemMessage(result.getRoomId(), "USER_LEFT", result);
            }

            log.info("[방 퇴장 완료] userId={}, roomId={}, nickname={}",
                result.getUserId(), result.getRoomId(), result.getNickname());
        } else {
            sendErrorResponse(sessionId, "", "ROOM_LEAVE_FAILED", result.getErrorMessage());
            log.error("[방 퇴장 실패] userId={}, roomId={}, error={}",
                result.getUserId(), result.getRoomId(), result.getErrorMessage());
        }
    }

    /**
     * 일반 오류 응답 전송 구현
     */
    @Override
    public void sendErrorResponse(String sessionId, String clientTempId, String errorType, String errorMessage) {
        try {
            Map<String, Object> response = Map.of(
                "clientTempId", clientTempId != null ? clientTempId : "",
                "type", errorType,
                "title", getErrorTitle(errorType),
                "detail", errorMessage,
                "timestamp", System.currentTimeMillis()
            );

            messagingTemplate.convertAndSendToUser(sessionId, "/queue/errors", response);
        } catch (Exception e) {
            log.error("[오류 응답 전송 실패] sessionId={}, errorType={}", sessionId, errorType, e);
        }
    }

    // === Private 헬퍼 메소드들 ===

    /**
     * 메시지 전송 성공 응답
     */
    private void sendSuccessResponse(String sessionId, MessageProcessResult result) {
        try {
            Map<String, Object> response = Map.of(
                "clientTempId", result.getClientTempId(),
                "messageId", result.getMessageId(),
                "status", "SUCCESS",
                "processingTimeMs", result.getProcessingTimeMs(),
                "timestamp", System.currentTimeMillis()
            );

            messagingTemplate.convertAndSendToUser(sessionId, "/queue/ack", response);
        } catch (Exception e) {
            log.error("[성공 응답 전송 실패] sessionId={}, messageId={}", sessionId, result.getMessageId(), e);
        }
    }

    /**
     * 방 입장 성공 응답
     */
    private void sendRoomJoinSuccessResponse(String sessionId, RoomJoinResult result) {
        try {
            Map<String, Object> response = Map.of(
                "type", "ROOM_JOIN_SUCCESS",
                "roomId", result.getRoomId(),
                "userId", result.getUserId(),
                "nickname", result.getNickname(),
                "message", "방에 성공적으로 입장했습니다",
                "timestamp", System.currentTimeMillis()
            );

            messagingTemplate.convertAndSendToUser(sessionId, "/queue/room-join-response", response);
            log.debug("[방 입장 성공 응답 전송] sessionId={}, roomId={}", sessionId, result.getRoomId());
        } catch (Exception e) {
            log.error("[방 입장 성공 응답 전송 실패] sessionId={}, roomId={}", sessionId, result.getRoomId(), e);
        }
    }

    /**
     * 방 퇴장 성공 응답
     */
    private void sendRoomLeaveSuccessResponse(String sessionId, RoomLeaveResult result) {
        try {
            Map<String, Object> response = Map.of(
                "type", "ROOM_LEAVE_SUCCESS",
                "roomId", result.getRoomId(),
                "userId", result.getUserId(),
                "nickname", result.getNickname(),
                "message", "방에서 성공적으로 퇴장했습니다",
                "timestamp", System.currentTimeMillis()
            );

            messagingTemplate.convertAndSendToUser(sessionId, "/queue/room-leave-response", response);
            log.info("[방 퇴장 성공 응답 전송] sessionId={}, roomId={}, response={}", sessionId, result.getRoomId(), response);
        } catch (Exception e) {
            log.error("[방 퇴장 성공 응답 전송 실패] sessionId={}, roomId={}", sessionId, result.getRoomId(), e);
        }
    }

    /**
     * 시스템 메시지 브로드캐스트 (제네릭)
     */
    private void broadcastSystemMessage(Long roomId, String messageType, Object result) {
        try {
            String content;
            Long userId;
            String nickname;

            if (result instanceof RoomJoinResult) {
                RoomJoinResult joinResult = (RoomJoinResult) result;
                content = joinResult.getMessage();
                userId = joinResult.getUserId();
                nickname = joinResult.getNickname();
            } else if (result instanceof RoomLeaveResult) {
                RoomLeaveResult leaveResult = (RoomLeaveResult) result;
                content = leaveResult.getMessage();
                userId = leaveResult.getUserId();
                nickname = leaveResult.getNickname();
            } else {
                log.warn("[시스템 메시지] 알 수 없는 결과 타입: {}", result.getClass().getSimpleName());
                return;
            }

            Map<String, Object> systemMessage = Map.of(
                "type", "SYSTEM_MESSAGE",
                "messageType", messageType,
                "roomId", roomId,
                "userId", userId,
                "nickname", nickname,
                "content", content,
                "timestamp", System.currentTimeMillis()
            );

            // 해당 방의 모든 구독자에게 시스템 메시지 브로드캐스트
            messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/messages", systemMessage);

            log.debug("[시스템 메시지] {} 브로드캐스트: roomId={}, nickname={}",
                messageType, roomId, nickname);

        } catch (Exception e) {
            log.error("[시스템 메시지] 브로드캐스트 실패: roomId={}, messageType={}", roomId, messageType, e);
        }
    }

    /**
     * 오류 타입별 제목 반환
     */
    private String getErrorTitle(String errorType) {
        switch (errorType) {
            case "AUTHENTICATION_REQUIRED":
                return "인증 필요";
            case "INVALID_PAYLOAD":
                return "잘못된 요청";
            case "MESSAGE_PROCESSING_FAILED":
                return "메시지 처리 실패";
            case "ROOM_JOIN_FAILED":
                return "방 입장 실패";
            case "ROOM_LEAVE_FAILED":
                return "방 퇴장 실패";
            case "INTERNAL_ERROR":
                return "내부 오류";
            default:
                return "알 수 없는 오류";
        }
    }
}