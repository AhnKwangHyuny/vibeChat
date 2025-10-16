package com.vibechat.service.room.participants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 방 참가자 수 조회 서비스 구현체
 * 
 * SRP: Redis 기반 참가자 수 조회만 담당
 * 
 * 최적화:
 * - 배치 조회로 N+1 문제 방지
 * - Redis Pipeline 활용 (향후)
 * 
 * Note:
 * - chat-server와 동일한 Redis 키 패턴 사용
 * - 읽기 전용 (CRUD는 chat-server가 담당)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoomParticipantCountServiceImpl implements RoomParticipantCountService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * chat-server와 동일한 키 패턴
     * Key: "room:{roomId}:participants"
     * Type: Set<userId>
     */
    private static final String ROOM_PARTICIPANTS_KEY = "room:{}:participants";

    @Override
    public int getParticipantCount(Long roomId) {
        try {
            String key = buildRoomParticipantsKey(roomId);
            Long count = redisTemplate.opsForSet().size(key);
            
            int result = count != null ? count.intValue() : 0;
            
            if (log.isDebugEnabled()) {
                log.debug("[ParticipantCount] roomId={}, count={}", roomId, result);
            }
            
            return result;

        } catch (Exception e) {
            log.error("[ParticipantCount] 조회 실패: roomId={}", roomId, e);
            return 0; // Fail-safe: 실패 시 0 반환
        }
    }

    @Override
    public Map<Long, Integer> getParticipantCounts(List<Long> roomIds) {
        log.debug("[ParticipantCount] 배치 조회: {} 건", roomIds.size());

        Map<Long, Integer> result = new HashMap<>();

        try {
            // TODO: Redis Pipeline 또는 executePipelined 사용으로 최적화
            // 현재는 순차 조회 (향후 개선)
            
            for (Long roomId : roomIds) {
                int count = getParticipantCount(roomId);
                result.put(roomId, count);
            }

            log.debug("[ParticipantCount] 배치 조회 완료: {} 건", result.size());

        } catch (Exception e) {
            log.error("[ParticipantCount] 배치 조회 실패", e);
        }

        return result;
    }

    @Override
    public boolean hasParticipantData(Long roomId) {
        try {
            String key = buildRoomParticipantsKey(roomId);
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));

        } catch (Exception e) {
            log.error("[ParticipantCount] 존재 여부 확인 실패: roomId={}", roomId, e);
            return false;
        }
    }

    /**
     * Redis 키 생성
     */
    private String buildRoomParticipantsKey(Long roomId) {
        return ROOM_PARTICIPANTS_KEY.replace("{}", roomId.toString());
    }

    /**
     * 배치 조회 최적화 (향후 구현)
     * 
     * Redis Pipeline 사용 예시:
     * 
     * <pre>
     * List<Object> results = redisTemplate.executePipelined(
     *     (RedisCallback<Object>) connection -> {
     *         for (Long roomId : roomIds) {
     *             byte[] key = buildRoomParticipantsKey(roomId).getBytes();
     *             connection.sCard(key);
     *         }
     *         return null;
     *     }
     * );
     * </pre>
     */
    @SuppressWarnings("unused")
    private Map<Long, Integer> getParticipantCountsWithPipeline(List<Long> roomIds) {
        // TODO: Pipeline 구현
        return new HashMap<>();
    }
}

