package com.vibechat.exception.room;

/**
 * 유효하지 않은 초대 코드로 방 입장 시도 시 발생하는 예외
 * 
 * HTTP Status: 400 BAD_REQUEST
 */
public class InvalidInviteCodeException extends RuntimeException {
    
    private final Long roomId;
    private final String providedCode;

    public InvalidInviteCodeException(Long roomId, String providedCode) {
        super(String.format("유효하지 않은 초대 코드입니다. roomId=%d", roomId));
        this.roomId = roomId;
        this.providedCode = providedCode;
    }

    public InvalidInviteCodeException(String message) {
        super(message);
        this.roomId = null;
        this.providedCode = null;
    }

    public Long getRoomId() {
        return roomId;
    }

    public String getProvidedCode() {
        return providedCode;
    }
}

