package com.vibechat.exception.user;

/**
 * 사용자를 찾을 수 없을 때 발생하는 예외
 *
 * HTTP Status: 404 NOT_FOUND
 */
public class UserNotFoundException extends RuntimeException {

    private final Long userId;

    public UserNotFoundException(Long userId) {
        super("사용자를 찾을 수 없습니다. ID: " + userId);
        this.userId = userId;
    }

    public UserNotFoundException(String message) {
        super(message);
        this.userId = null;
    }

    public Long getUserId() {
        return userId;
    }
}
