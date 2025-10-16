package com.vibechat.exception.room;

/**
 * 방을 찾을 수 없을 때 발생하는 예외
 * 
 * HTTP Status: 404 NOT_FOUND
 */
public class RoomNotFoundException extends RuntimeException {
    
    private final Long roomId;

    public RoomNotFoundException(Long roomId) {
        super("채팅방을 찾을 수 없습니다. ID: " + roomId);
        this.roomId = roomId;
    }

    public RoomNotFoundException(String message) {
        super(message);
        this.roomId = null;
    }

    public Long getRoomId() {
        return roomId;
    }
}

