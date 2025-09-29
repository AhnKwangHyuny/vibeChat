package com.vibechat.websocket.controller;

import com.vibechat.service.websocket.WebSocketRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

/**
 * 채팅방 관리 WebSocket 컨트롤러
 *
 * Clean Architecture 원칙에 따라 비즈니스 로직 없이 순수한 요청 라우팅만 수행:
 * - 요청 파라미터 수집
 * - 서비스 호출
 * - 결과 반환
 */
@Controller
@RequiredArgsConstructor
public class RoomController {

    private final WebSocketRoomService webSocketRoomService;

    /**
     * 방 입장 처리
     *
     * 엔드포인트: /app/rooms/{roomId}/join
     */
    @MessageMapping("/rooms/{roomId}/join")
    public void joinRoom(@DestinationVariable Long roomId,
                         SimpMessageHeaderAccessor headerAccessor) {
        webSocketRoomService.handleJoinRoom(roomId, headerAccessor);
    }

    /**
     * 방 퇴장 처리
     *
     * 엔드포인트: /app/rooms/{roomId}/leave
     */
    @MessageMapping("/rooms/{roomId}/leave")
    public void leaveRoom(@DestinationVariable Long roomId,
                          SimpMessageHeaderAccessor headerAccessor) {
        webSocketRoomService.handleLeaveRoom(roomId, headerAccessor);
    }
}