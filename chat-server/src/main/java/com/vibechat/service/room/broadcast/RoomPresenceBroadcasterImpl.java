package com.vibechat.service.room.broadcast;

import com.vibechat.event.RoomPresenceUpdateEvent;
import com.vibechat.service.room.RoomParticipantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 방 Presence 브로드캐스트 전담 서비스
 *
 * SRP: 방의 온라인 사용자 수를 WebSocket으로 브로드캐스트하는 책임만 담당
 * - RoomPresenceUpdateEvent 수신 시 자동 실행
 * - 이벤트 기반 아키텍처로 느슨한 결합 달성
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoomPresenceBroadcasterImpl implements RoomPresentBroadCaster {

    private final RoomParticipantService roomParticipantService;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    @Override
    public void handlePresenceUpdateEvent(RoomPresenceUpdateEvent event) {
        try {
            broadcastPresence(event.getRoomId());
            
            log.info("[Presence 이벤트 처리] roomId={}, reason={}, userId={}", 
                event.getRoomId(), event.getReason().getDescription(), event.getUserId());

        } catch (Exception e) {
            log.error("[Presence 이벤트 처리 실패] roomId={}, event={}", 
                event.getRoomId(), event, e);
        }
    }

    /**
     * 방의 현재 참가자 수와 목록을 모든 구독자에게 브로드캐스트
     *
     * @param roomId 방 ID
     */
    private void broadcastPresence(Long roomId) {
        try {
            // 1. 방 참가자 수 및 목록 조회
            int participantCount = roomParticipantService.getParticipantCount(roomId);
            List<Long> participantIds = roomParticipantService.getParticipants(roomId);

            // 2. Presence 이벤트 구성 (참가자 ID 목록 포함)
            // TODO: 참가자 상세 정보 (nickname, avatarUrl) 추가 필요
            Map<String, Object> presenceEvent = Map.of(
                "count", participantCount,
                "participantIds", participantIds,
                "timestamp", System.currentTimeMillis()
            );

            // 3. WebSocket 브로드캐스트
            String destination = String.format("/topic/rooms/%d/presence", roomId);
            messagingTemplate.convertAndSend(destination, presenceEvent);

            log.info("[Presence 브로드캐스트] roomId={}, participantCount={}, participantIds={}, destination={}",
                    roomId, participantCount, participantIds, destination);
        } catch (Exception e) {
            log.error("[Presence 브로드캐스트 실패] roomId={}", roomId, e);
        }
    }

}

