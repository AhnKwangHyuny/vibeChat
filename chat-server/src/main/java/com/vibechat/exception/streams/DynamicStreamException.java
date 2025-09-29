package com.vibechat.exception.streams;

/**
 * 동적 스트림 관리 전용 예외
 *
 * 스트림 생성, 삭제, Consumer Group 관리 중 발생하는 오류를 캡슐화
 */
public class DynamicStreamException extends RuntimeException {

    public DynamicStreamException(String message) {
        super(message);
    }

    public DynamicStreamException(String message, Throwable cause) {
        super(message, cause);
    }
}