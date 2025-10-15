package com.vibechat.event;

import com.vibechat.domain.PresenceUpdateReason;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 방 Presence 업데이트 이벤트
 *
 * 도메인 이벤트: 방의 참가자 변경을 알림
 * - 이벤트 발행: WebSocketConnectionEventService, WebSocketEventHandler
 * - 이벤트 수신: RoomPresenceBroadcaster
 *
 */
@Getter
public class RoomPresenceUpdateEvent {

    private final Long roomId;
    private final PresenceUpdateReason reason;
    private final Long userId;
    private final LocalDateTime timestamp;

    private RoomPresenceUpdateEvent(Long roomId, PresenceUpdateReason reason, Long userId, LocalDateTime timestamp) {
        this.roomId = roomId;
        this.reason = reason;
        this.userId = userId;
        this.timestamp = timestamp;
    }

    /**
     * 사용자 입장 이벤트 생성
     */
    public static RoomPresenceUpdateEvent userJoined(Long roomId, Long userId) {
        return new RoomPresenceUpdateEvent(roomId, PresenceUpdateReason.USER_JOINED, userId, LocalDateTime.now());
    }

    /**
     * 사용자 퇴장 이벤트 생성
     */
    public static RoomPresenceUpdateEvent userLeft(Long roomId, Long userId) {
        return new RoomPresenceUpdateEvent(roomId, PresenceUpdateReason.USER_LEFT, userId, LocalDateTime.now());
    }

    /**
     * 연결 끊김 이벤트 생성
     */
    public static RoomPresenceUpdateEvent connectionLost(Long roomId, Long userId) {
        return new RoomPresenceUpdateEvent(roomId, PresenceUpdateReason.CONNECTION_LOST, userId, LocalDateTime.now());
    }

    /**
     * 강제 퇴장 이벤트 생성
     */
    public static RoomPresenceUpdateEvent userKicked(Long roomId, Long userId) {
        return new RoomPresenceUpdateEvent(roomId, PresenceUpdateReason.USER_KICKED, userId, LocalDateTime.now());
    }

    @Override
    public String toString() {
        return String.format("RoomPresenceUpdateEvent{roomId=%d, reason=%s, userId=%d, timestamp=%s}",
            roomId, reason, userId, timestamp);
    }
}

