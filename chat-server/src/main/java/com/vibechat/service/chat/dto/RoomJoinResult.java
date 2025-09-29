package com.vibechat.service.chat.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 방 입장 결과 DTO
 *
 * 비즈니스 로직 처리 결과와 UI 응답에 필요한 데이터를 캡슐화
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RoomJoinResult {
    private final boolean success;
    private final Long roomId;
    private final Long userId;
    private final String nickname;
    private final String message;
    private final String errorMessage;

    /**
     * 성공 결과 생성
     */
    public static RoomJoinResult success(Long roomId, Long userId, String nickname, String systemMessage) {
        return new RoomJoinResult(true, roomId, userId, nickname, systemMessage, null);
    }

    /**
     * 실패 결과 생성
     */
    public static RoomJoinResult failure(Long roomId, Long userId, String nickname, String errorMessage) {
        return new RoomJoinResult(false, roomId, userId, nickname, null, errorMessage);
    }

    /**
     * 시스템 메시지가 있는지 확인
     */
    public boolean hasSystemMessage() {
        return success && message != null && !message.trim().isEmpty();
    }
}