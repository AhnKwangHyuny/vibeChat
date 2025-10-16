package com.vibechat.exception.room;

/**
 * 방 생성 실패 시 발생하는 예외
 * 
 * HTTP Status: 500 INTERNAL_SERVER_ERROR
 */
public class RoomCreationException extends RuntimeException {

    public RoomCreationException(String message) {
        super(message);
    }

    public RoomCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}

