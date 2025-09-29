package com.vibechat.service.websocket;

import com.vibechat.utils.session.StomSessionUtil;
import com.vibechat.websocket.session.WebSocketSessionInfo;
import com.vibechat.service.chat.ChatRoomOperations;
import com.vibechat.service.chat.ChatResponseHandler;
import com.vibechat.service.chat.dto.RoomJoinResult;
import com.vibechat.service.chat.dto.RoomLeaveResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Service;

/**
 * WebSocket 채팅방 관리 서비스
 *
 * Clean Architecture 원칙에 따라 컨트롤러의 모든 비즈니스 로직을 처리:
 * - 세션 정보 추출 및 검증
 * - 인증 상태 확인
 * - 방 입장/퇴장 비즈니스 로직 처리
 * - 예외 처리 및 에러 응답
 * - 로깅
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketRoomService {

    private final ChatRoomOperations chatRoomOperations;
    private final ChatResponseHandler chatResponseHandler;
    private final WebSocketExceptionHandler webSocketExceptionHandler;

    /**
     * 방 입장 처리 - 모든 비즈니스 로직 포함
     */
    public void handleJoinRoom(Long roomId, SimpMessageHeaderAccessor headerAccessor) {
        try {
            String sessionId = headerAccessor.getSessionId();
            WebSocketSessionInfo sessionInfo = StomSessionUtil.extractSessionInfo(headerAccessor);
            // 인증 검증
            if (sessionInfo == null) {
                webSocketExceptionHandler.handleAuthenticationError(sessionId, "", "방 입장");
                return;
            }

            log.info("[방 입장 요청] userId={}, roomId={}, sessionId={}",
                sessionInfo.getUserId(), roomId, sessionId);

            // 비즈니스 로직 호출
            RoomJoinResult result = chatRoomOperations.joinRoom(roomId, sessionInfo);

            // 응답 처리
            chatResponseHandler.handleRoomJoinResult(sessionId, result);

            log.info("[방 입장 처리 완료] userId={}, roomId={}, success={}",
                sessionInfo.getUserId(), roomId, result.isSuccess());

        } catch (Exception e) {
            webSocketExceptionHandler.handleRoomJoinError(headerAccessor.getSessionId(), roomId, e);
        }
    }

    /**
     * 방 퇴장 처리 - 모든 비즈니스 로직 포함
     */
    public void handleLeaveRoom(Long roomId, SimpMessageHeaderAccessor headerAccessor) {
        try {
            String sessionId = headerAccessor.getSessionId();
            WebSocketSessionInfo sessionInfo = StomSessionUtil.extractSessionInfo(headerAccessor);

            // 인증 검증
            if (sessionInfo == null) {
                webSocketExceptionHandler.handleAuthenticationError(sessionId, "", "방 퇴장");
                return;
            }

            log.info("[방 퇴장 요청] userId={}, roomId={}, sessionId={}",
                sessionInfo.getUserId(), roomId, sessionId);

            // 비즈니스 로직 호출
            RoomLeaveResult result = chatRoomOperations.leaveRoom(roomId, sessionInfo);

            // 응답 처리
            chatResponseHandler.handleRoomLeaveResult(sessionId, result);

            log.info("[방 퇴장 처리 완료] userId={}, roomId={}, success={}",
                sessionInfo.getUserId(), roomId, result.isSuccess());

        } catch (Exception e) {
            webSocketExceptionHandler.handleRoomLeaveError(headerAccessor.getSessionId(), roomId, e);
        }
    }
}