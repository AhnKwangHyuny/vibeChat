package com.vibechat.exception.room;

/**
 * 방 접근 권한이 없을 때 발생하는 예외
 * 
 * HTTP Status: 403 FORBIDDEN
 */
public class RoomAccessDeniedException extends RuntimeException {
    
    private final Long roomId;
    private final Long userId;
    private final String action;

    public RoomAccessDeniedException(Long roomId, Long userId, String action) {
        super(String.format("방 '%s' 권한이 없습니다. roomId=%d, userId=%d", action, roomId, userId));
        this.roomId = roomId;
        this.userId = userId;
        this.action = action;
    }

    public RoomAccessDeniedException(String message) {
        super(message);
        this.roomId = null;
        this.userId = null;
        this.action = null;
    }

    public Long getRoomId() {
        return roomId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getAction() {
        return action;
    }
}

