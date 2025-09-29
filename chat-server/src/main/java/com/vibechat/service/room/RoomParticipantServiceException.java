package com.vibechat.service.room;

/**
 * 방 참가자 서비스 전용 예외
 *
 * 방 참가자 관리 중 발생하는 오류를 캡슐화
 */
public class RoomParticipantServiceException extends RuntimeException {

    public RoomParticipantServiceException(String message) {
        super(message);
    }

    public RoomParticipantServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}