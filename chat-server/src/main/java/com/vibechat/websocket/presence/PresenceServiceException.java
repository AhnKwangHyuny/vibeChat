package com.vibechat.websocket.presence;

/**
 * Presence 서비스 전용 예외
 *
 * Presence 상태 관리 중 발생하는 오류를 캡슐화
 */
public class PresenceServiceException extends RuntimeException {

    public PresenceServiceException(String message) {
        super(message);
    }

    public PresenceServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}