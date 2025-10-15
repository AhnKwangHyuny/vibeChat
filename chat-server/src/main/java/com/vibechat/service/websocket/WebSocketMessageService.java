package com.vibechat.service.websocket;

import com.vibechat.dto.SendMessagePayload;
import com.vibechat.dto.coordinator.MessageProcessResult;
import com.vibechat.service.coordinator.MessageCoordinatorService;
import com.vibechat.utils.session.StomSessionUtil;
import com.vibechat.websocket.session.WebSocketSessionInfo;
import com.vibechat.service.chat.ChatResponseHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Service;

/**
 * WebSocket 채팅 메시지 처리 서비스
 *
 * Clean Architecture 원칙에 따라 컨트롤러의 모든 비즈니스 로직을 처리:
 * - 세션 정보 추출 및 검증
 * - 메시지 처리 비즈니스 로직 호출
 * - 예외 처리 및 에러 응답
 * - 로깅
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketMessageService {

    private final MessageCoordinatorService messageCoordinatorService;
    private final ChatResponseHandler chatResponseHandler;
    private final WebSocketExceptionHandler webSocketExceptionHandler;

    /**
     * 채팅 메시지 전송 처리 - 모든 비즈니스 로직 포함
     */
    public void handleSendMessage(Long roomId, SendMessagePayload payload, SimpMessageHeaderAccessor headerAccessor) {
        try {
            String sessionId = headerAccessor.getSessionId();
            WebSocketSessionInfo sessionInfo = StomSessionUtil.extractSessionInfo(headerAccessor);

            log.info("[메시지 전송 요청] userId={}, roomId={}, clientTempId={}, type={}",
                sessionInfo != null ? sessionInfo.getUserId() : "unknown",
                roomId, payload.getClientTempId(), payload.getType());

            // 인증 검증
            if (sessionInfo == null) {
                webSocketExceptionHandler.handleAuthenticationError(sessionId, payload.getClientTempId(), "메시지 전송");
                return;
            }

            // 메시지 처리 비즈니스 로직 호출 (세션 정보 포함)
            MessageProcessResult result = messageCoordinatorService.processRoomMessage(
                roomId, sessionInfo, payload);

            // 응답 처리
            chatResponseHandler.handleMessageResult(sessionId, result, payload.getClientTempId());

            log.info("[메시지 전송 처리 완료] userId={}, roomId={}, clientTempId={}, success={}",
                sessionInfo.getUserId(), roomId, payload.getClientTempId(), result.isSuccess());

        } catch (Exception e) {
            webSocketExceptionHandler.handleMessageSendError(
                headerAccessor.getSessionId(), payload.getClientTempId(), roomId, e);
        }
    }
}