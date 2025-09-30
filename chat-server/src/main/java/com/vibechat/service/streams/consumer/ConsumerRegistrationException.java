package com.vibechat.service.streams.consumer;

/**
 * Consumer Group 등록 관련 예외
 */
public class ConsumerRegistrationException extends RuntimeException {

    public ConsumerRegistrationException(String message) {
        super(message);
    }

    public ConsumerRegistrationException(String message, Throwable cause) {
        super(message, cause);
    }
}