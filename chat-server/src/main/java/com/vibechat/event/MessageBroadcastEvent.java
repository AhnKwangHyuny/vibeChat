package com.vibechat.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 메시지를 WebSocket을 통해 브로드캐스트해야 할 때 발생하는 내부 이벤트입니다.
 * 이 이벤트는 StreamMessageProcessor에 의해 발행되고, WebSocketBroadcastService에 의해 처리됩니다.
 */
@Getter
public class MessageBroadcastEvent extends ApplicationEvent {

    private final String destination;
    private final Object payload;

    /**
     * @param source 이벤트를 발생시킨 소스 객체
     * @param destination 메시지를 보낼 WebSocket 목적지 (e.g., "/topic/rooms/123")
     * @param payload 전송할 메시지 데이터
     */
    public MessageBroadcastEvent(Object source, String destination, Object payload) {
        super(source);
        this.destination = destination;
        this.payload = payload;
    }
}
