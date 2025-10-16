package com.vibechat.service.room;

import com.vibechat.event.RoomPresenceUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

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
public class RoomPresenceBroadcaster {

    private final RoomParticipantService roomParticipantService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Presence 업데이트 이벤트 수신 핸들러
     *
     * @EventListener: Spring이 이벤트를 자동으로 연결
     * - 비동기 처리 가능 (@Async 추가 시)
     * - 트랜잭션 분리
     * - 순환 참조 없음 (이벤트 기반)
     */
    @EventListener
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
     * 방의 현재 참가자 수를 모든 구독자에게 브로드캐스트
     *
     * @param roomId 방 ID
     */
    private void broadcastPresence(Long roomId) {
        try {
            // 1. 방 참가자 수 조회
            int participantCount = roomParticipantService.getParticipantCount(roomId);

            // 2. Presence 이벤트 구성
            Map<String, Object> presenceEvent = Map.of(
                "count", participantCount,
                "timestamp", System.currentTimeMillis()
            );

            // 3. WebSocket 브로드캐스트
            String destination = String.format("/topic/rooms/%d/presence", roomId);
            messagingTemplate.convertAndSend(destination, presenceEvent);

            log.info("[Presence 브로드캐스트] roomId={}, participantCount={}", roomId, participantCount);

        } catch (Exception e) {
            log.error("[Presence 브로드캐스트 실패] roomId={}", roomId, e);
            // 브로드캐스트 실패가 주요 비즈니스 로직에 영향을 주지 않도록 예외를 재발생시키지 않음
        }
    }

}

