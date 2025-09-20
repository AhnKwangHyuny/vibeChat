package com.vibechat.exception.auth;

/**
 * 사용자가 로그인되지 않았을 때 발생하는 예외
 * - 401 Unauthorized로 매핑
 */
public class UserNotLoggedInException extends RuntimeException {
    public UserNotLoggedInException(String message) {
        super(message);
    }
}