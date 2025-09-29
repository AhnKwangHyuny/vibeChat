package com.vibechat.service.streams;

import com.vibechat.exception.streams.DynamicStreamException;
import com.vibechat.service.streams.consumer.ConsumerRegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 동적 스트림 관리 서비스 구현체
 *
 * DIP(의존성 역전 원칙) 준수를 위해 DynamicStreamOperations 인터페이스 구현
 * 새로운 방 생성 시 해당 방 전용 Redis Stream을 자동으로 생성하고
 * Consumer Group에 등록하는 엔터프라이즈급 동적 스트림 관리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DynamicStreamService implements DynamicStreamOperations {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ConsumerRegistrationService consumerRegistrationService;

    // Redis Stream 키 패턴
    private static final String ROOM_STREAM_PATTERN = "stream:room:{}";
    private static final String USER_STREAM_PATTERN = "stream:user:{}";

    /**
     * 방 스트림 존재 확인 및 생성
     *
     * @param roomId 방 ID
     */
    @Override
    public void ensureRoomStream(Long roomId) {
        try {
            String streamKey = ROOM_STREAM_PATTERN.replace("{}", roomId.toString());

            log.info("[DynamicStream] 방 스트림 확인 시작: roomId={}, streamKey={}", roomId, streamKey);

            // 1. Redis Stream 존재 확인
            if (!redisStreamExists(streamKey)) {
                // 2. 새 스트림 생성
                createRedisStream(streamKey);

                // 3. Consumer Group에 동적 등록
                registerConsumerGroupForRoom(roomId, streamKey);

                log.info("[DynamicStream] 새 방 스트림 생성 완료: roomId={}, streamKey={}", roomId, streamKey);
            } else {
                log.info("[DynamicStream] 방 스트림 이미 존재: roomId={}, streamKey={}", roomId, streamKey);
            }

        } catch (Exception e) {
            log.error("[DynamicStream] 방 스트림 생성 실패: roomId={}", roomId, e);
            throw new DynamicStreamException("방 스트림 생성에 실패했습니다", e);
        }
    }

    /**
     * 사용자 스트림 존재 확인 및 생성
     *
     * @param userId 사용자 ID
     */
    @Override
    public void ensureUserStream(Long userId) {
        try {
            String streamKey = USER_STREAM_PATTERN.replace("{}", userId.toString());

            log.debug("[DynamicStream] 사용자 스트림 확인 시작: userId={}, streamKey={}", userId, streamKey);

            if (!redisStreamExists(streamKey)) {
                createRedisStream(streamKey);
                registerConsumerGroupForUser(userId, streamKey);

                log.info("[DynamicStream] 새 사용자 스트림 생성 완료: userId={}, streamKey={}", userId, streamKey);
            } else {
                log.debug("[DynamicStream] 사용자 스트림 이미 존재: userId={}, streamKey={}", userId, streamKey);
            }

        } catch (Exception e) {
            log.error("[DynamicStream] 사용자 스트림 생성 실패: userId={}", userId, e);
            throw new DynamicStreamException("사용자 스트림 생성에 실패했습니다", e);
        }
    }

    /**
     * Redis Stream 존재 여부 확인
     */
    private boolean redisStreamExists(String streamKey) {
        try {
            // XINFO STREAM 명령으로 스트림 존재 확인
            return redisTemplate.hasKey(streamKey);
        } catch (Exception e) {
            log.warn("[DynamicStream] 스트림 존재 확인 실패: streamKey={}", streamKey, e);
            return false;
        }
    }

    /**
     * Redis Stream 생성
     */
    private void createRedisStream(String streamKey) {
        try {
            // 빈 메시지로 스트림 초기화 (스트림이 생성됨)
            Map<String, String> initialEntry = Map.of("init", "true");

            redisTemplate.opsForStream().add(streamKey , initialEntry);

            log.debug("[DynamicStream] Redis Stream 생성 완료: streamKey={}", streamKey);
        } catch (Exception e) {
            log.error("[DynamicStream] Redis Stream 생성 실패: streamKey={}", streamKey, e);
            throw new DynamicStreamException("Redis Stream 생성에 실패했습니다: " + streamKey, e);
        }
    }

    /**
     * 방 스트림을 Consumer Group에 등록
     */
    private void registerConsumerGroupForRoom(Long roomId, String streamKey) {
        try {
            // 모든 Consumer Group에 새 방 스트림 추가
            consumerRegistrationService.addRoomStreamToConsumerGroups(streamKey);

            log.info("[DynamicStream] 방 스트림 Consumer Group 등록 완료: roomId={}, streamKey={}", roomId, streamKey);
        } catch (Exception e) {
            log.error("[DynamicStream] 방 스트림 Consumer Group 등록 실패: roomId={}, streamKey={}", roomId, streamKey, e);
            throw new DynamicStreamException("Consumer Group 등록에 실패했습니다", e);
        }
    }

    /**
     * 사용자 스트림을 Consumer Group에 등록
     */
    private void registerConsumerGroupForUser(Long userId, String streamKey) {
        try {
            // 사용자 관련 Consumer Group에 새 사용자 스트림 추가
            consumerRegistrationService.addUserStreamToConsumerGroups(streamKey);

            log.info("[DynamicStream] 사용자 스트림 Consumer Group 등록 완료: userId={}, streamKey={}", userId, streamKey);
        } catch (Exception e) {
            log.error("[DynamicStream] 사용자 스트림 Consumer Group 등록 실패: userId={}, streamKey={}", userId, streamKey, e);
            throw new DynamicStreamException("사용자 Consumer Group 등록에 실패했습니다", e);
        }
    }

    /**
     * 방 스트림 정리 (방 삭제 시 사용)
     */
    @Override
    public void cleanupRoomStream(Long roomId) {
        try {
            String streamKey = ROOM_STREAM_PATTERN.replace("{}", roomId.toString());

            log.info("[DynamicStream] 방 스트림 정리 시작: roomId={}, streamKey={}", roomId, streamKey);

            // Consumer Group에서 스트림 제거
            consumerRegistrationService.removeRoomStreamFromConsumerGroups(streamKey);

            // Redis에서 스트림 삭제 (실제 운영에서는 아카이빙 고려)
            redisTemplate.delete(streamKey);

            log.info("[DynamicStream] 방 스트림 정리 완료: roomId={}, streamKey={}", roomId, streamKey);
        } catch (Exception e) {
            log.error("[DynamicStream] 방 스트림 정리 실패: roomId={}", roomId, e);
        }
    }

    /**
     * 스트림 통계 정보 조회
     */
    @Override
    public StreamStats getStreamStats(String streamKey) {
        try {
            Long length = redisTemplate.opsForStream().size(streamKey);
            return new StreamStats(streamKey, length != null ? length : 0L);
        } catch (Exception e) {
            log.warn("[DynamicStream] 스트림 통계 조회 실패: streamKey={}", streamKey, e);
            return new StreamStats(streamKey, 0L);
        }
    }

    /**
     * 스트림 통계 정보 클래스
     */
    public static class StreamStats {
        private final String streamKey;
        private final long messageCount;

        public StreamStats(String streamKey, long messageCount) {
            this.streamKey = streamKey;
            this.messageCount = messageCount;
        }

        public String getStreamKey() { return streamKey; }
        public long getMessageCount() { return messageCount; }

        @Override
        public String toString() {
            return String.format("StreamStats{streamKey='%s', messageCount=%d}", streamKey, messageCount);
        }
    }
}