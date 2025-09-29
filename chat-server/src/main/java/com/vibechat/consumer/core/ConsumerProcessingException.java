package com.vibechat.consumer.core;

/**
 * Consumer 메시지 처리 실패 예외
 *
 * Consumer에서 메시지 처리 중 발생하는 모든 예외의 기반 클래스
 */
public class ConsumerProcessingException extends RuntimeException {

    private final String consumerType;
    private final String messageId;
    private final boolean retryable;

    public ConsumerProcessingException(String consumerType, String messageId, String message) {
        this(consumerType, messageId, message, null, true);
    }

    public ConsumerProcessingException(String consumerType, String messageId, String message, boolean retryable) {
        this(consumerType, messageId, message, null, retryable);
    }

    public ConsumerProcessingException(String consumerType, String messageId, String message, Throwable cause) {
        this(consumerType, messageId, message, cause, true);
    }

    public ConsumerProcessingException(String consumerType, String messageId, String message, Throwable cause, boolean retryable) {
        super(String.format("[%s] 메시지 처리 실패 (ID: %s): %s", consumerType, messageId, message), cause);
        this.consumerType = consumerType;
        this.messageId = messageId;
        this.retryable = retryable;
    }

    public String getConsumerType() {
        return consumerType;
    }

    public String getMessageId() {
        return messageId;
    }

    public boolean isRetryable() {
        return retryable;
    }
}