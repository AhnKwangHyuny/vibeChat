package com.vibechat.service;

import com.vibechat.exception.RateLimitExceededException;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * 사용자+방 단위 레이트 제한 서비스 (인메모리, 토큰 버킷)
 * - 키: rl:msg:{userId}:{roomId}
 * - 버킷: 용량 20, 분당 20 토큰 보충, 초기 토큰 10
 */
@Service
@Slf4j
public class RateLimitService {

    private static final String KEY_FORMAT = "rl:msg:%d:%d";
    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    private TokenBucket getBucket(long userId, long roomId) {
        String key = KEY_FORMAT.formatted(userId, roomId);
        return buckets.computeIfAbsent(key, k -> new TokenBucket(20, 20.0 / 60.0, 10));
    }

    /**
     * 레이트 제한 체크 (예외 발생 방식)
     *
     * @param userId 사용자 ID
     * @param roomId 방 ID
     * @throws RateLimitExceededException 제한 초과 시
     */
    public void checkRateLimit(Long userId, Long roomId) {
        if (!tryConsume(userId, roomId)) {
            long waitNanos = nanosToWait(userId, roomId);
            long waitSeconds = Math.max(1, waitNanos / 1_000_000_000);

            log.warn("레이트 제한 초과: userId={}, roomId={}, waitSeconds={}", userId, roomId, waitSeconds);
            throw new RateLimitExceededException(
                "메시지 전송 제한을 초과했습니다. " + waitSeconds + "초 후 다시 시도해주세요.",
                waitSeconds
            );
        }
    }

    public boolean tryConsume(long userId, long roomId) {
        return getBucket(userId, roomId).tryConsume(1);
    }

    public long nanosToWait(long userId, long roomId) {
        return getBucket(userId, roomId).nanosToNextToken();
    }

    private static final class TokenBucket {
        private final int capacity;
        private final double tokensPerSecond; // 20 / 60
        private double tokens;
        private long lastRefillNanos;

        private TokenBucket(int capacity, double tokensPerSecond, int initialTokens) {
            this.capacity = capacity;
            this.tokensPerSecond = tokensPerSecond;
            this.tokens = Math.min(initialTokens, capacity);
            this.lastRefillNanos = System.nanoTime();
        }

        private synchronized void refill() {
            long now = System.nanoTime();
            long elapsedNanos = now - lastRefillNanos;
            if (elapsedNanos <= 0) return;
            double add = (elapsedNanos / 1_000_000_000.0) * tokensPerSecond;
            if (add > 0) {
                tokens = Math.min(capacity, tokens + add);
                lastRefillNanos = now;
            }
        }

        private synchronized boolean tryConsume(int permits) {
            refill();
            if (tokens >= permits) {
                tokens -= permits;
                return true;
            }
            return false;
        }

        private synchronized long nanosToNextToken() {
            refill();
            if (tokens >= 1.0) return 0L;
            // 부족한 토큰 1.0 - tokens 만큼을 채우는데 필요한 시간
            double deficit = 1.0 - tokens;
            double seconds = deficit / tokensPerSecond;
            return (long) (seconds * 1_000_000_000L);
        }
    }
}


