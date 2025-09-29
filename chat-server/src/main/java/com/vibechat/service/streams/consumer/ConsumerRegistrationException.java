package com.vibechat.service.streams.consumer;

/**
 * Consumer 등록 관리 전용 예외
 *
 * Consumer Group 등록, 해제, 동적 스트림 관리 중 발생하는 오류를 캡슐화
 */
public class ConsumerRegistrationException extends RuntimeException {

    public ConsumerRegistrationException(String message) {
        super(message);
    }

    public ConsumerRegistrationException(String message, Throwable cause) {
        super(message, cause);
    }
}