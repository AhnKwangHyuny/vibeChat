package com.vibechat.exception;

/**
 * 레이트 제한 초과 예외
 * - 429로 매핑되며, 재시도까지 남은 초(retryAfterSeconds)를 제공한다.
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


