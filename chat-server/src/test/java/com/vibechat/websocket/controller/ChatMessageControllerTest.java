package com.vibechat.websocket.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.vibechat.dto.SendMessagePayload;
import com.vibechat.dto.coordinator.MessageProcessResult;
import com.vibechat.service.coordinator.MessageCoordinatorService;
import com.vibechat.utils.session.StomSessionUtil;
import com.vibechat.websocket.session.WebSocketSessionInfo;
import com.vibechat.service.chat.ChatResponseHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

/**
 * ChatMessageController 단위 테스트
 *
 * 테스트 범위:
 * - 메시지 전송 성공 시나리오
 * - 메시지 전송 실패 시나리오
 * - 세션 정보 추출 실패 시나리오
 * - 예외 처리 시나리오
 */
@ExtendWith(MockitoExtension.class)
class ChatMessageControllerTest {

    @Mock
    private MessageCoordinatorService messageCoordinatorService;

    @Mock
    private ChatResponseHandler chatResponseHandler;

    private ChatMessageController chatMessageController;

    @BeforeEach
    void setUp() {
        chatMessageController = new ChatMessageController(messageCoordinatorService, chatResponseHandler);
    }

    @Test
    void 메시지_전송_성공_시나리오() {
        // Given
        Long roomId = 1L;
        String sessionId = "session-123";
        String clientTempId = "temp-456";
        Long userId = 10L;

        SendMessagePayload payload = new SendMessagePayload();
        payload.setClientTempId(clientTempId);
        payload.setType("TEXT");
        payload.setContent("Hello World");

        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create();
        headerAccessor.setSessionId(sessionId);

        WebSocketSessionInfo sessionInfo = new WebSocketSessionInfo(userId, "testUser", null);
        MessageProcessResult successResult = MessageProcessResult.success(789L, clientTempId, 50L);

        try (MockedStatic<StomSessionUtil> mockedStatic = mockStatic(StomSessionUtil.class)) {
            // 세션 정보 추출 모킹
            mockedStatic.when(() -> StomSessionUtil.extractSessionInfo(headerAccessor))
                    .thenReturn(sessionInfo);

            // 메시지 처리 성공 모킹
            when(messageCoordinatorService.processRoomMessage(roomId, userId, payload))
                    .thenReturn(successResult);

            // When
            chatMessageController.sendMessage(roomId, payload, headerAccessor);

            // Then
            verify(messageCoordinatorService, times(1))
                    .processRoomMessage(roomId, userId, payload);
            verify(chatResponseHandler, times(1))
                    .handleMessageResult(sessionId, successResult, clientTempId);
            verify(chatResponseHandler, never())
                    .sendErrorResponse(any(), any(), any(), any());
        }
    }

    @Test
    void 메시지_전송_실패_시나리오() {
        // Given
        Long roomId = 1L;
        String sessionId = "session-123";
        String clientTempId = "temp-456";
        Long userId = 10L;

        SendMessagePayload payload = new SendMessagePayload();
        payload.setClientTempId(clientTempId);
        payload.setType("TEXT");
        payload.setContent("Hello World");

        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create();
        headerAccessor.setSessionId(sessionId);

        WebSocketSessionInfo sessionInfo = new WebSocketSessionInfo(userId, "testUser", null);
        MessageProcessResult failureResult = MessageProcessResult.failure(clientTempId, "Processing failed");

        try (MockedStatic<StomSessionUtil> mockedStatic = mockStatic(StomSessionUtil.class)) {
            // 세션 정보 추출 모킹
            mockedStatic.when(() -> StomSessionUtil.extractSessionInfo(headerAccessor))
                    .thenReturn(sessionInfo);

            // 메시지 처리 실패 모킹
            when(messageCoordinatorService.processRoomMessage(roomId, userId, payload))
                    .thenReturn(failureResult);

            // When
            chatMessageController.sendMessage(roomId, payload, headerAccessor);

            // Then
            verify(messageCoordinatorService, times(1))
                    .processRoomMessage(roomId, userId, payload);
            verify(chatResponseHandler, times(1))
                    .handleMessageResult(sessionId, failureResult, clientTempId);
        }
    }

    @Test
    void 세션_정보_추출_실패_시나리오() {
        // Given
        Long roomId = 1L;
        String sessionId = "session-123";
        String clientTempId = "temp-456";

        SendMessagePayload payload = new SendMessagePayload();
        payload.setClientTempId(clientTempId);

        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create();
        headerAccessor.setSessionId(sessionId);

        try (MockedStatic<StomSessionUtil> mockedStatic = mockStatic(StomSessionUtil.class)) {
            // 세션 정보 추출 실패 모킹 (null 반환)
            mockedStatic.when(() -> StomSessionUtil.extractSessionInfo(headerAccessor))
                    .thenReturn(null);

            // When
            chatMessageController.sendMessage(roomId, payload, headerAccessor);

            // Then - NullPointerException으로 인해 catch 블록 실행됨
            verify(messageCoordinatorService, never())
                    .processRoomMessage(any(), any(), any());
            verify(chatResponseHandler, times(1))
                    .sendErrorResponse(sessionId, clientTempId, "INTERNAL_ERROR", "메시지 처리 중 오류가 발생했습니다");
        }
    }

    @Test
    void 메시지_코디네이터_예외_발생_시나리오() {
        // Given
        Long roomId = 1L;
        String sessionId = "session-123";
        String clientTempId = "temp-456";
        Long userId = 10L;

        SendMessagePayload payload = new SendMessagePayload();
        payload.setClientTempId(clientTempId);

        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create();
        headerAccessor.setSessionId(sessionId);

        WebSocketSessionInfo sessionInfo = new WebSocketSessionInfo(userId, "testUser", null);

        try (MockedStatic<StomSessionUtil> mockedStatic = mockStatic(StomSessionUtil.class)) {
            // 세션 정보 추출 모킹
            mockedStatic.when(() -> StomSessionUtil.extractSessionInfo(headerAccessor))
                    .thenReturn(sessionInfo);

            // 메시지 코디네이터에서 예외 발생 모킹
            when(messageCoordinatorService.processRoomMessage(roomId, userId, payload))
                    .thenThrow(new RuntimeException("Database connection failed"));

            // When
            chatMessageController.sendMessage(roomId, payload, headerAccessor);

            // Then
            verify(messageCoordinatorService, times(1))
                    .processRoomMessage(roomId, userId, payload);
            verify(chatResponseHandler, times(1))
                    .sendErrorResponse(sessionId, clientTempId, "INTERNAL_ERROR", "메시지 처리 중 오류가 발생했습니다");
            verify(chatResponseHandler, never())
                    .handleMessageResult(any(), any(), any());
        }
    }
}