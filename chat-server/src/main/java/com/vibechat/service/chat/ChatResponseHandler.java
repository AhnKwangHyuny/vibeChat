package com.vibechat.service.chat;

import com.vibechat.service.chat.dto.RoomJoinResult;
import com.vibechat.service.chat.dto.RoomLeaveResult;
import com.vibechat.dto.coordinator.MessageProcessResult;

/**
 * 채팅 응답 전송 인터페이스
 *
 * 역할:
 * - WebSocket 응답 메시지 포맷팅 및 전송 추상화
 * - 성공/실패 응답 처리 추상화
 * - 시스템 메시지 브로드캐스트 추상화
 */
public interface ChatResponseHandler {

    /**
     * 메시지 전송 결과 응답 처리
     *
     * @param sessionId WebSocket 세션 ID
     * @param result 메시지 처리 결과
     * @param clientTempId 클라이언트 임시 ID
     *
     * 동작:
     * - 성공 시: ACK 응답 전송 및 성공 로그
     * - 실패 시: 오류 응답 전송 및 실패 로그
     */
    void handleMessageResult(String sessionId, MessageProcessResult result, String clientTempId);

    /**
     * 방 입장 결과 응답 처리
     *
     * @param sessionId WebSocket 세션 ID
     * @param result 방 입장 결과
     *
     * 동작:
     * - 성공 시: 방 입장 성공 응답 + 시스템 메시지 브로드캐스트
     * - 실패 시: 오류 응답 전송
     */
    void handleRoomJoinResult(String sessionId, RoomJoinResult result);

    /**
     * 방 퇴장 결과 응답 처리
     *
     * @param sessionId WebSocket 세션 ID
     * @param result 방 퇴장 결과
     *
     * 동작:
     * - 성공 시: 방 퇴장 성공 응답 + 시스템 메시지 브로드캐스트
     * - 실패 시: 오류 응답 전송
     */
    void handleRoomLeaveResult(String sessionId, RoomLeaveResult result);

    /**
     * 일반 오류 응답 전송
     *
     * @param sessionId WebSocket 세션 ID
     * @param clientTempId 클라이언트 임시 ID
     * @param errorType 오류 타입 (AUTHENTICATION_REQUIRED, INTERNAL_ERROR 등)
     * @param errorMessage 오류 메시지
     *
     * 동작:
     * - 오류 타입에 맞는 제목과 메시지로 포맷팅
     * - /queue/errors 채널로 오류 응답 전송
     */
    void sendErrorResponse(String sessionId, String clientTempId, String errorType, String errorMessage);
}