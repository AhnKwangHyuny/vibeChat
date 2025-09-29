package com.vibechat.websocket.connection;

import com.vibechat.consumer.notification.NotificationConsumer;
import com.vibechat.websocket.presence.WebSocketPresenceService;
import com.vibechat.websocket.session.WebSocketSessionInfo;
import com.vibechat.service.room.RoomParticipantService;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * WebSocket 연결/해제 이벤트 처리 서비스
 *
 * SRP: 연결 이벤트에 따른 후속 처리만 담당
 * OCP: 새로운 이벤트 처리 로직 확장 가능
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketConnectionEventService {

    private final WebSocketPresenceService presenceService;
    private final NotificationConsumer notificationConsumer;
    private final RoomParticipantService roomParticipantService;

    /**
     * 사용자 연결 시 후속 처리
     */
    public void handleUserConnected(WebSocketSessionInfo sessionInfo) {
        try {
            // 1. Presence 상태 등록
            presenceService.registerUserConnection(sessionInfo);

            // 2. 대기 중인 오프라인 알림 처리
//            handleOfflineNotifications(sessionInfo.getUserId());

            // 3. 연결 완료 로그
            log.info("[사용자 연결 처리 완료] userId={}, sessionId={}, nickname={}",
                sessionInfo.getUserId(), sessionInfo.getSessionId(), sessionInfo.getNickname());

        } catch (Exception e) {
            log.error("[사용자 연결 처리 실패] userId={}, sessionId={}",
                sessionInfo.getUserId(), sessionInfo.getSessionId(), e);
            throw new WebSocketConnectionException("사용자 연결 처리에 실패했습니다", e);
        }
    }

    /**
     * 사용자 연결 해제 시 후속 처리
     */
    public void handleUserDisconnected(String sessionId) {
        try {
            // 1. 세션 정보 조회
            WebSocketSessionInfo sessionInfo = presenceService.getSessionInfo(sessionId);
            if (sessionInfo == null) {
                log.warn("[연결 해제 처리] 세션 정보를 찾을 수 없음: sessionId={}", sessionId);
                return;
            }

            // 2. Presence 상태 정리
            presenceService.unregisterUserConnection(sessionId);

            // 3. 완전 오프라인 확인 및 처리
            if (!presenceService.isUserOnline(sessionInfo.getUserId())) {
                handleUserGoesOffline(sessionInfo);
            }

            // 4. 연결 해제 완료 로그
            log.info("[사용자 연결 해제 처리 완료] userId={}, sessionId={}, nickname={}",
                sessionInfo.getUserId(), sessionId, sessionInfo.getNickname());

        } catch (Exception e) {
            log.error("[사용자 연결 해제 처리 실패] sessionId={}", sessionId, e);
            throw new WebSocketConnectionException("사용자 연결 해제 처리에 실패했습니다", e);
        }
    }

    /**
     * 사용자가 온라인 상태가 될 때 대기 중인 알림 처리
     */
    private void handleOfflineNotifications(Long userId) {
        try {
            // 오프라인 상태에서 쌓인 알림들을 처리
            notificationConsumer.processPendingNotifications(userId);

            log.debug("[오프라인 알림 처리 완료] userId={}", userId);
        } catch (Exception e) {
            log.warn("[오프라인 알림 처리 실패] userId={}", userId, e);
            // 알림 처리 실패가 연결에 영향을 주지 않도록 예외를 던지지 않음
        }
    }

    /**
     * 사용자가 완전 오프라인 상태가 될 때 처리
     */
    private void handleUserGoesOffline(WebSocketSessionInfo sessionInfo) {
        try {
            Long userId = sessionInfo.getUserId();

            // ** 핵심: 모든 참가 중인 방에서 사용자 제거 **
            handleUserLeaveAllRooms(userId, sessionInfo);

            // 향후 확장: 오프라인 상태 전환 시 필요한 처리
            // 예: 마지막 접속 시간 기록, 장기간 오프라인 사용자 정리 등

            log.info("[사용자 완전 오프라인] userId={}, nickname={}, 세션 지속시간={}분",
                userId, sessionInfo.getNickname(),
                sessionInfo.getSessionDurationMinutes());

        } catch (Exception e) {
            log.warn("[오프라인 전환 처리 실패] userId={}", sessionInfo.getUserId(), e);
        }
    }

    /**
     * 사용자가 모든 방에서 나가는 처리 (방 퇴장 로직)
     *
     * 사용자가 완전 오프라인이 될 때 모든 방에서 제거
     */
    private void handleUserLeaveAllRooms(Long userId, WebSocketSessionInfo sessionInfo) {
        try {
            // 1. 사용자가 참가 중인 방 목록 조회
            List<Long> userRooms = roomParticipantService.getUserRooms(userId);

            if (userRooms.isEmpty()) {
                log.debug("[방 퇴장 처리] 참가 중인 방이 없음: userId={}", userId);
                return;
            }

            log.info("[방 퇴장 처리 시작] userId={}, nickname={}, 참가 중인 방 개수={}",
                userId, sessionInfo.getNickname(), userRooms.size());

            // 2. 각 방에서 사용자 제거
            int successCount = 0;
            for (Long roomId : userRooms) {
                try {
                    // 방에서 참가자 제거
                    roomParticipantService.removeParticipant(roomId, userId);
                    successCount++;

                    // 방 퇴장 시스템 메시지 전송 (선택적)
                    sendRoomLeaveSystemMessage(roomId, sessionInfo);

                    log.debug("[방 퇴장 완료] userId={}, roomId={}", userId, roomId);

                } catch (Exception e) {
                    log.warn("[방 퇴장 실패] userId={}, roomId={}", userId, roomId, e);
                    // 개별 방 퇴장 실패가 전체 처리를 중단시키지 않도록 continue
                }
            }

            log.info("[방 퇴장 처리 완료] userId={}, nickname={}, 성공={}/{}",
                userId, sessionInfo.getNickname(), successCount, userRooms.size());

        } catch (Exception e) {
            log.error("[방 퇴장 처리 실패] userId={}", userId, e);
            // 방 퇴장 실패가 연결 해제를 방해하지 않도록 예외를 다시 던지지 않음
        }
    }

    /**
     * 방 퇴장 시스템 메시지 전송 (선택적 구현)
     */
    private void sendRoomLeaveSystemMessage(Long roomId, WebSocketSessionInfo sessionInfo) {
        try {
            // TODO: 시스템 메시지 전송 로직 구현
            // 예: "{nickname}님이 나갔습니다" 메시지를 방의 나머지 참가자에게 전송

            log.debug("[시스템 메시지] {}님이 방 {}에서 나갔습니다",
                sessionInfo.getNickname(), roomId);

        } catch (Exception e) {
            log.warn("[방 퇴장 시스템 메시지 전송 실패] roomId={}, userId={}",
                roomId, sessionInfo.getUserId(), e);
        }
    }

    /**
     * 연결 상태 통계 조회
     */
    public ConnectionStats getConnectionStats() {
        try {
            var presenceStats = presenceService.getPresenceStats();

            return new ConnectionStats(
                presenceStats.getActiveUsers(),
                presenceStats.getActiveSessions(),
                System.currentTimeMillis()
            );

        } catch (Exception e) {
            log.warn("[연결 통계 조회 실패]", e);
            return new ConnectionStats(0, 0, System.currentTimeMillis());
        }
    }

    /**
     * 단일 방 퇴장 처리 (공개 API - 외부에서 호출 가능)
     * 예: REST API를 통한 명시적 방 나가기
     */
    public void handleUserLeaveRoom(Long userId, Long roomId, String reason) {
        try {
            boolean wasParticipant = roomParticipantService.isParticipant(roomId, userId);

            if (wasParticipant) {
                roomParticipantService.removeParticipant(roomId, userId);
                log.info("[단일 방 퇴장] userId={}, roomId={}, reason={}", userId, roomId, reason);

                // TODO: 시스템 메시지 전송 (사용자 정보 조회 후)
            } else {
                log.debug("[단일 방 퇴장] 이미 참가자가 아님: userId={}, roomId={}", userId, roomId);
            }
        } catch (Exception e) {
            log.error("[단일 방 퇴장 실패] userId={}, roomId={}", userId, roomId, e);
            throw new WebSocketConnectionException("방 퇴장 처리에 실패했습니다", e);
        }
    }

    /**
     * 연결 통계 정보 클래스
     */
    public static class ConnectionStats {
        private final int activeUsers;
        private final int activeSessions;
        private final long timestamp;

        public ConnectionStats(int activeUsers, int activeSessions, long timestamp) {
            this.activeUsers = activeUsers;
            this.activeSessions = activeSessions;
            this.timestamp = timestamp;
        }

        public int getActiveUsers() { return activeUsers; }
        public int getActiveSessions() { return activeSessions; }
        public long getTimestamp() { return timestamp; }

        public double getAverageSessionsPerUser() {
            return activeUsers > 0 ? (double) activeSessions / activeUsers : 0.0;
        }

        @Override
        public String toString() {
            return String.format("ConnectionStats{activeUsers=%d, activeSessions=%d, avgSessions=%.2f}",
                activeUsers, activeSessions, getAverageSessionsPerUser());
        }
    }
}