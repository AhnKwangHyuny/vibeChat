package com.vibechat.exception.auth;

/**
 * 로그아웃 처리 중 오류가 발생했을 때의 예외
 * - 500 Internal Server Error로 매핑
 */
public class LogoutProcessException extends RuntimeException {
    public LogoutProcessException(String message, Throwable cause) {
        super(message, cause);
    }
}