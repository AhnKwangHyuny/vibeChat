package com.vibechat.exception;

/**
 * 메시지 전송 제한 초과 예외
 *
 * 사용자가 너무 많은 메시지를 보낼 때 발생
 */
public class RateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}


