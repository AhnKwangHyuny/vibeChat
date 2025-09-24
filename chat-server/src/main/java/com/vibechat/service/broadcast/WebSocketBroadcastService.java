package com.vibechat.service.broadcast;

import com.vibechat.event.MessageBroadcastEvent;

/**
 * WebSocket 메시지 브로드캐스트 서비스 인터페이스
 */
public interface WebSocketBroadcastService {

    /**
     * MessageBroadcastEvent를 받아 처리합니다.
     * @param event 브로드캐스트할 메시지 정보를 담은 이벤트
     */
    void handleMessageBroadcast(MessageBroadcastEvent event);
}
