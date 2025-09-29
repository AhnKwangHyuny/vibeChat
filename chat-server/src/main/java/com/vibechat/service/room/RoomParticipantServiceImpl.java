package com.vibechat.service.room;

import com.vibechat.websocket.presence.WebSocketPresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Redis 기반 방 참가자 관리 서비스 구현체
 *
 * SRP: 방 참가자 관리만 담당
 * 기존 WebSocketPresenceService와 호환성 유지
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoomParticipantServiceImpl implements RoomParticipantService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final WebSocketPresenceService presenceService;

    private static final String ROOM_PARTICIPANTS_KEY = "room:{}:participants";
    private static final String USER_ROOMS_KEY = "user:{}:rooms";
    private static final int PARTICIPANT_TTL_HOURS = 24; // 24시간 TTL

    @Override
    public void addParticipant(Long roomId, Long userId) {
        try {
            String roomParticipantsKey = ROOM_PARTICIPANTS_KEY.replace("{}", roomId.toString());
            String userRoomsKey = USER_ROOMS_KEY.replace("{}", userId.toString());

            // 양방향 관계 설정
            redisTemplate.opsForSet().add(roomParticipantsKey, userId.toString());
            redisTemplate.opsForSet().add(userRoomsKey, roomId.toString());

            // TTL 설정
            redisTemplate.expire(roomParticipantsKey, PARTICIPANT_TTL_HOURS, TimeUnit.HOURS);
            redisTemplate.expire(userRoomsKey, PARTICIPANT_TTL_HOURS, TimeUnit.HOURS);

            log.info("[RoomParticipant] 참가자 추가 완료: roomId={}, userId={}", roomId, userId);

        } catch (Exception e) {
            log.error("[RoomParticipant] 참가자 추가 실패: roomId={}, userId={}", roomId, userId, e);
            throw new RoomParticipantServiceException("참가자 추가에 실패했습니다", e);
        }
    }

    @Override
    public void removeParticipant(Long roomId, Long userId) {
        try {
            String roomParticipantsKey = ROOM_PARTICIPANTS_KEY.replace("{}", roomId.toString());
            String userRoomsKey = USER_ROOMS_KEY.replace("{}", userId.toString());

            // 양방향 관계 해제
            redisTemplate.opsForSet().remove(roomParticipantsKey, userId.toString());
            redisTemplate.opsForSet().remove(userRoomsKey, roomId.toString());

            log.info("[RoomParticipant] 참가자 제거 완료: roomId={}, userId={}", roomId, userId);

        } catch (Exception e) {
            log.error("[RoomParticipant] 참가자 제거 실패: roomId={}, userId={}", roomId, userId, e);
            throw new RoomParticipantServiceException("참가자 제거에 실패했습니다", e);
        }
    }

    @Override
    public List<Long> getParticipants(Long roomId) {
        try {
            String roomParticipantsKey = ROOM_PARTICIPANTS_KEY.replace("{}", roomId.toString());
            Set<Object> participantIds = redisTemplate.opsForSet().members(roomParticipantsKey);

            if (participantIds == null) {
                return List.of();
            }

            return participantIds.stream()
                .map(Object::toString)
                .map(Long::parseLong)
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("[RoomParticipant] 참가자 목록 조회 실패: roomId={}", roomId, e);
            return List.of(); // 빈 목록 반환으로 안전하게 처리
        }
    }

    @Override
    public int getParticipantCount(Long roomId) {
        try {
            String roomParticipantsKey = ROOM_PARTICIPANTS_KEY.replace("{}", roomId.toString());
            Long count = redisTemplate.opsForSet().size(roomParticipantsKey);
            return count != null ? count.intValue() : 0;

        } catch (Exception e) {
            log.error("[RoomParticipant] 참가자 수 조회 실패: roomId={}", roomId, e);
            return 0;
        }
    }

    @Override
    public boolean isParticipant(Long roomId, Long userId) {
        try {
            String roomParticipantsKey = ROOM_PARTICIPANTS_KEY.replace("{}", roomId.toString());
            return redisTemplate.opsForSet().isMember(roomParticipantsKey, userId.toString());

        } catch (Exception e) {
            log.error("[RoomParticipant] 참가자 확인 실패: roomId={}, userId={}", roomId, userId, e);
            return false;
        }
    }

    @Override
    public List<Long> getUserRooms(Long userId) {
        try {
            String userRoomsKey = USER_ROOMS_KEY.replace("{}", userId.toString());
            Set<Object> roomIds = redisTemplate.opsForSet().members(userRoomsKey);

            if (roomIds == null) {
                return List.of();
            }

            return roomIds.stream()
                .map(Object::toString)
                .map(Long::parseLong)
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("[RoomParticipant] 사용자 방 목록 조회 실패: userId={}", userId, e);
            return List.of();
        }
    }

    @Override
    public void cleanupInactiveParticipants(Long roomId) {
        try {
            List<Long> participants = getParticipants(roomId);
            int removedCount = 0;

            for (Long participantId : participants) {
                // WebSocketPresenceService를 통해 온라인 상태 확인
                if (!presenceService.isUserOnline(participantId)) {
                    removeParticipant(roomId, participantId);
                    removedCount++;
                }
            }

            if (removedCount > 0) {
                log.info("[RoomParticipant] 비활성 참가자 정리 완료: roomId={}, removed={}",
                    roomId, removedCount);
            }

        } catch (Exception e) {
            log.error("[RoomParticipant] 비활성 참가자 정리 실패: roomId={}", roomId, e);
        }
    }

    /**
     * 방 참가자 통계 정보
     */
    @Override
    public RoomParticipantStats getStats(Long roomId) {
        try {
            List<Long> participants = getParticipants(roomId);
            int totalCount = participants.size();
            int onlineCount = 0;

            for (Long participantId : participants) {
                if (presenceService.isUserOnline(participantId)) {
                    onlineCount++;
                }
            }

            return new RoomParticipantStats(roomId, totalCount, onlineCount);

        } catch (Exception e) {
            log.error("[RoomParticipant] 통계 조회 실패: roomId={}", roomId, e);
            return new RoomParticipantStats(roomId, 0, 0);
        }
    }
}