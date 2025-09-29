package com.vibechat.websocket.presence;

import com.vibechat.websocket.session.WebSocketSessionInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket Presence 상태 관리 서비스
 *
 * SRP: 사용자 온라인/오프라인 상태 관리만 담당
 * DIP: StringRedisTemplate 추상화에 의존 (타입 안정성)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketPresenceService {

    private final StringRedisTemplate redisTemplate;

    // Redis 키 패턴 상수화
    private static final String PRESENCE_SESSION_KEY = "presence:session:{}";
    private static final String PRESENCE_USER_KEY = "presence:user:{}";
    private static final int SESSION_TTL_HOURS = 1;

    /**
     * 사용자 연결 시 Presence 상태 등록
     */
    public void registerUserConnection(WebSocketSessionInfo sessionInfo) {
        try {
            String sessionKey = PRESENCE_SESSION_KEY.replace("{}", sessionInfo.getSessionId());
            String userKey = PRESENCE_USER_KEY.replace("{}", sessionInfo.getUserId().toString());


            // 1. 세션별 정보 저장 - 모든 값을 String으로 변환
            Map<String, String> sessionData = Map.of(
                "userId", sessionInfo.getUserId().toString(),
                "nickname", sessionInfo.getNickname(),
                "avatarUrl", sessionInfo.getAvatarUrl() != null ? sessionInfo.getAvatarUrl() : "",
                "connectedAt", sessionInfo.getConnectedAt().toString()
            );

            redisTemplate.opsForHash().putAll(sessionKey, sessionData);
            redisTemplate.expire(sessionKey, SESSION_TTL_HOURS, TimeUnit.HOURS);

            // 2. 사용자별 활성 세션 등록 (Set으로 관리)
            redisTemplate.opsForSet().add(userKey, sessionInfo.getSessionId());
            redisTemplate.expire(userKey, SESSION_TTL_HOURS, TimeUnit.HOURS);

            log.info("[Presence 등록 완료] userId={}, sessionId={}, nickname={}",
                sessionInfo.getUserId(), sessionInfo.getSessionId(), sessionInfo.getNickname());

        } catch (Exception e) {
            log.error("[Presence 등록 실패] userId={}, sessionId={}",
                sessionInfo.getUserId(), sessionInfo.getSessionId(), e);
            throw new PresenceServiceException("사용자 연결 상태 등록에 실패했습니다", e);
        }
    }

    /**
     * 사용자 연결 해제 시 Presence 상태 정리
     */
    public void unregisterUserConnection(String sessionId) {
        try {
            String sessionKey = PRESENCE_SESSION_KEY.replace("{}", sessionId);

            // 1. 세션 정보 조회
            Map<Object, Object> sessionData = redisTemplate.opsForHash().entries(sessionKey);
            if (sessionData.isEmpty()) {
                log.warn("[Presence 정리 불가] 세션 정보를 찾을 수 없음: sessionId={}", sessionId);
                return;
            }

            Object userIdObj = sessionData.get("userId");
            if (userIdObj == null) {
                log.warn("[Presence 정리 불가] 사용자 ID가 없음: sessionId={}", sessionId);
                return;
            }

            String userId = userIdObj.toString();
            String userKey = PRESENCE_USER_KEY.replace("{}", userId);

            // 2. 사용자별 활성 세션에서 제거
            redisTemplate.opsForSet().remove(userKey, sessionId);

            // 3. 세션 정보 삭제
            redisTemplate.delete(sessionKey);

            // 4. 사용자의 모든 세션이 종료되었는지 확인
            Long remainingSessions = redisTemplate.opsForSet().size(userKey);
            if (remainingSessions != null && remainingSessions == 0) {
                // 모든 세션 종료 시 사용자 Presence 키도 삭제
                redisTemplate.delete(userKey);
                log.info("[사용자 완전 오프라인] userId={}", userId);
            }

            log.info("[Presence 정리 완료] userId={}, sessionId={}, 남은 세션 수={}",
                userId, sessionId, remainingSessions != null ? remainingSessions : 0);

        } catch (Exception e) {
            log.error("[Presence 정리 실패] sessionId={}", sessionId, e);
            throw new PresenceServiceException("사용자 연결 상태 정리에 실패했습니다", e);
        }
    }

    /**
     * 사용자 온라인 상태 확인
     */
    public boolean isUserOnline(Long userId) {
        try {
            String userKey = PRESENCE_USER_KEY.replace("{}", userId.toString());
            Long sessionCount = redisTemplate.opsForSet().size(userKey);
            return sessionCount != null && sessionCount > 0;
        } catch (Exception e) {
            log.warn("[온라인 상태 확인 실패] userId={}", userId, e);
            return false; // 오류 시 오프라인으로 간주
        }
    }

    /**
     * 사용자의 활성 세션 수 조회
     */
    public int getActiveSessionCount(Long userId) {
        try {
            String userKey = PRESENCE_USER_KEY.replace("{}", userId.toString());
            Long sessionCount = redisTemplate.opsForSet().size(userKey);
            return sessionCount != null ? sessionCount.intValue() : 0;
        } catch (Exception e) {
            log.warn("[활성 세션 수 조회 실패] userId={}", userId, e);
            return 0;
        }
    }

    /**
     * 세션 ID로 사용자 정보 조회
     */
    public WebSocketSessionInfo getSessionInfo(String sessionId) {
        try {
            String sessionKey = PRESENCE_SESSION_KEY.replace("{}", sessionId);
            Map<Object, Object> sessionData = redisTemplate.opsForHash().entries(sessionKey);

            if (sessionData.isEmpty()) {
                return null;
            }

            Object userIdObj = sessionData.get("userId");
            Object nicknameObj = sessionData.get("nickname");
            Object avatarUrlObj = sessionData.get("avatarUrl");
            Object connectedAtObj = sessionData.get("connectedAt");

            if (userIdObj == null || nicknameObj == null) {
                return null;
            }

            return WebSocketSessionInfo.builder()
                .sessionId(sessionId)
                .userId(Long.parseLong(userIdObj.toString()))
                .nickname(nicknameObj.toString())
                .avatarUrl(avatarUrlObj != null ? avatarUrlObj.toString() : null)
                .connectedAt(connectedAtObj != null ?
                    java.time.LocalDateTime.parse(connectedAtObj.toString()) :
                    java.time.LocalDateTime.now())
                .build();

        } catch (Exception e) {
            log.warn("[세션 정보 조회 실패] sessionId={}", sessionId, e);
            return null;
        }
    }

    /**
     * Presence 시스템 헬스체크
     */
    public boolean isHealthy() {
        try {
            // Redis 연결 확인
            redisTemplate.opsForValue().get("healthcheck");
            return true;
        } catch (Exception e) {
            log.error("[Presence 헬스체크 실패] Redis 연결 불가", e);
            return false;
        }
    }

    /**
     * Presence 통계 정보
     */
    public PresenceStats getPresenceStats() {
        try {
            // 활성 사용자 수 계산 (presence:user:* 키 개수)
            var userKeys = redisTemplate.keys("presence:user:*");
            int activeUsers = userKeys != null ? userKeys.size() : 0;

            // 전체 세션 수 계산 (presence:session:* 키 개수)
            var sessionKeys = redisTemplate.keys("presence:session:*");
            int activeSessions = sessionKeys != null ? sessionKeys.size() : 0;

            return new PresenceStats(activeUsers, activeSessions);
        } catch (Exception e) {
            log.warn("[Presence 통계 조회 실패]", e);
            return new PresenceStats(0, 0);
        }
    }

    /**
     * Presence 통계 정보 클래스
     */
    public static class PresenceStats {
        private final int activeUsers;
        private final int activeSessions;

        public PresenceStats(int activeUsers, int activeSessions) {
            this.activeUsers = activeUsers;
            this.activeSessions = activeSessions;
        }

        public int getActiveUsers() { return activeUsers; }
        public int getActiveSessions() { return activeSessions; }

        @Override
        public String toString() {
            return String.format("PresenceStats{activeUsers=%d, activeSessions=%d}",
                activeUsers, activeSessions);
        }
    }
}