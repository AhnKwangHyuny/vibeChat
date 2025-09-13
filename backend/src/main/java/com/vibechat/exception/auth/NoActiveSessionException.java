package com.vibechat.exception.auth;

/**
 * 활성 세션이 없을 때 발생하는 예외
 * - 400 Bad Request로 매핑
 */
public class NoActiveSessionException extends RuntimeException {
    public NoActiveSessionException(String message) {
        super(message);
    }
}