package com.vibechat.websocket.controller;

import com.vibechat.dto.SendMessagePayload;
import com.vibechat.service.websocket.WebSocketMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

/**
 * 채팅 메시지 전송 WebSocket 컨트롤러
 *
 * Clean Architecture 원칙에 따라 비즈니스 로직 없이 순수한 요청 라우팅만 수행:
 * - 요청 파라미터 수집
 * - 서비스 호출
 * - 결과 반환
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatMessageController {

    private final WebSocketMessageService webSocketMessageService;

    /**
     * 채팅 메시지 전송 처리
     *
     * 엔드포인트: /app/rooms/{roomId}/send
     */
    @MessageMapping("/rooms/{roomId}/send")
    public void sendMessage(@DestinationVariable Long roomId,
                            @Payload SendMessagePayload payload,
                            SimpMessageHeaderAccessor headerAccessor) {

        log.info("[DEBUG] ChatMessageController 메시지 수신 - roomId={}, type={}, content={}, clientTempId={}",
                roomId, payload.getType(), payload.getContentText(), payload.getClientTempId());

        webSocketMessageService.handleSendMessage(roomId, payload, headerAccessor);
    }
}