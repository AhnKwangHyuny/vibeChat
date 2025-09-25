package com.vibechat.exception;

/**
 * 메시지 검증 실패 예외
 *
 * 메시지 내용, 타입, 길이 등 검증 오류 시 발생
 */
public class MessageValidationException extends RuntimeException {

    public MessageValidationException(String message) {
        super(message);
    }

    public MessageValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}