package com.vibechat.service.broadcast;

import com.vibechat.event.MessageBroadcastEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * WebSocket 메시지 브로드캐스트 서비스 구현체
 */
@Slf4j
// @Service 어노테이션 제거 - Configuration에서 수동 등록
public class WebSocketBroadcastServiceImpl implements WebSocketBroadcastService {

    private SimpMessagingTemplate messagingTemplate;

    public void setMessagingTemplate(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * MessageBroadcastEvent를 구독하여, 실제 WebSocket 클라이언트에게 메시지를 전송합니다.
     * @param event 브로드캐스트할 메시지 정보를 담은 이벤트
     */
    @Override
    @EventListener
    public void handleMessageBroadcast(MessageBroadcastEvent event) {
        String destination = event.getDestination();
        Object payload = event.getPayload();

        try {
            messagingTemplate.convertAndSend(destination, payload);
            log.debug("Successfully broadcasted event payload to destination: {}", destination);
        } catch (Exception e) {
            log.error("Failed to broadcast message to destination: {}. Error: {}", destination, e.getMessage());
        }
    }
}
