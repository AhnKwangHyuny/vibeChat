package com.vibechat.websocket.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.vibechat.utils.session.StomSessionUtil;
import com.vibechat.websocket.session.WebSocketSessionInfo;
import com.vibechat.service.chat.ChatRoomOperations;
import com.vibechat.service.chat.ChatResponseHandler;
import com.vibechat.service.chat.dto.RoomJoinResult;
import com.vibechat.service.chat.dto.RoomLeaveResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

/**
 * RoomController 단위 테스트
 *
 * 테스트 범위:
 * - 방 입장 성공 시나리오
 * - 방 입장 실패 시나리오
 * - 방 퇴장 성공 시나리오
 * - 방 퇴장 실패 시나리오
 * - 인증 실패 시나리오
 * - 예외 처리 시나리오
 */
@ExtendWith(MockitoExtension.class)
class RoomControllerTest {

    @Mock
    private ChatRoomOperations chatRoomOperations;

    @Mock
    private ChatResponseHandler chatResponseHandler;

    private RoomController roomController;

    @BeforeEach
    void setUp() {
        roomController = new RoomController(chatRoomOperations, chatResponseHandler);
    }

    @Test
    void 방_입장_성공_시나리오() {
        // Given
        Long roomId = 1L;
        String sessionId = "session-123";
        Long userId = 10L;
        String nickname = "testUser";

        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create();
        headerAccessor.setSessionId(sessionId);

        WebSocketSessionInfo sessionInfo = new WebSocketSessionInfo(userId, nickname, null);
        RoomJoinResult successResult = RoomJoinResult.success(roomId, userId, nickname, "방에 입장했습니다");

        try (MockedStatic<StomSessionUtil> mockedStatic = mockStatic(StomSessionUtil.class)) {
            // 세션 정보 추출 모킹
            mockedStatic.when(() -> StomSessionUtil.extractSessionInfo(headerAccessor))
                    .thenReturn(sessionInfo);

            // 방 입장 성공 모킹
            when(chatRoomOperations.joinRoom(roomId, sessionInfo))
                    .thenReturn(successResult);

            // When
            roomController.joinRoom(roomId, headerAccessor);

            // Then
            verify(chatRoomOperations, times(1))
                    .joinRoom(roomId, sessionInfo);
            verify(chatResponseHandler, times(1))
                    .handleRoomJoinResult(sessionId, successResult);
            verify(chatResponseHandler, never())
                    .sendErrorResponse(any(), any(), any(), any());
        }
    }

    @Test
    void 방_입장_인증_실패_시나리오() {
        // Given
        Long roomId = 1L;
        String sessionId = "session-123";

        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create();
        headerAccessor.setSessionId(sessionId);

        try (MockedStatic<StomSessionUtil> mockedStatic = mockStatic(StomSessionUtil.class)) {
            // 세션 정보 추출 실패 모킹 (null 반환)
            mockedStatic.when(() -> StomSessionUtil.extractSessionInfo(headerAccessor))
                    .thenReturn(null);

            // When
            roomController.joinRoom(roomId, headerAccessor);

            // Then
            verify(chatRoomOperations, never())
                    .joinRoom(any(), any());
            verify(chatResponseHandler, times(1))
                    .sendErrorResponse(sessionId, "", "AUTHENTICATION_REQUIRED", "인증된 사용자만 방에 입장할 수 있습니다");
        }
    }

    @Test
    void 방_입장_비즈니스_로직_실패_시나리오() {
        // Given
        Long roomId = 1L;
        String sessionId = "session-123";
        Long userId = 10L;
        String nickname = "testUser";

        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create();
        headerAccessor.setSessionId(sessionId);

        WebSocketSessionInfo sessionInfo = new WebSocketSessionInfo(userId, nickname, null);
        RoomJoinResult failureResult = RoomJoinResult.failure(roomId, userId, nickname, "방 입장에 실패했습니다");

        try (MockedStatic<StomSessionUtil> mockedStatic = mockStatic(StomSessionUtil.class)) {
            // 세션 정보 추출 모킹
            mockedStatic.when(() -> StomSessionUtil.extractSessionInfo(headerAccessor))
                    .thenReturn(sessionInfo);

            // 방 입장 실패 모킹
            when(chatRoomOperations.joinRoom(roomId, sessionInfo))
                    .thenReturn(failureResult);

            // When
            roomController.joinRoom(roomId, headerAccessor);

            // Then
            verify(chatRoomOperations, times(1))
                    .joinRoom(roomId, sessionInfo);
            verify(chatResponseHandler, times(1))
                    .handleRoomJoinResult(sessionId, failureResult);
        }
    }

    @Test
    void 방_입장_예외_발생_시나리오() {
        // Given
        Long roomId = 1L;
        String sessionId = "session-123";
        Long userId = 10L;
        String nickname = "testUser";

        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create();
        headerAccessor.setSessionId(sessionId);

        WebSocketSessionInfo sessionInfo = new WebSocketSessionInfo(userId, nickname, null);

        try (MockedStatic<StomSessionUtil> mockedStatic = mockStatic(StomSessionUtil.class)) {
            // 세션 정보 추출 모킹
            mockedStatic.when(() -> StomSessionUtil.extractSessionInfo(headerAccessor))
                    .thenReturn(sessionInfo);

            // 방 입장 시 예외 발생 모킹
            when(chatRoomOperations.joinRoom(roomId, sessionInfo))
                    .thenThrow(new RuntimeException("Stream creation failed"));

            // When
            roomController.joinRoom(roomId, headerAccessor);

            // Then
            verify(chatRoomOperations, times(1))
                    .joinRoom(roomId, sessionInfo);
            verify(chatResponseHandler, times(1))
                    .sendErrorResponse(sessionId, "", "ROOM_JOIN_FAILED", "방 입장에 실패했습니다: Stream creation failed");
        }
    }

    @Test
    void 방_퇴장_성공_시나리오() {
        // Given
        Long roomId = 1L;
        String sessionId = "session-123";
        Long userId = 10L;
        String nickname = "testUser";

        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create();
        headerAccessor.setSessionId(sessionId);

        WebSocketSessionInfo sessionInfo = new WebSocketSessionInfo(userId, nickname, null);
        RoomLeaveResult successResult = RoomLeaveResult.success(roomId, userId, nickname, "방에서 나갔습니다");

        try (MockedStatic<StomSessionUtil> mockedStatic = mockStatic(StomSessionUtil.class)) {
            // 세션 정보 추출 모킹
            mockedStatic.when(() -> StomSessionUtil.extractSessionInfo(headerAccessor))
                    .thenReturn(sessionInfo);

            // 방 퇴장 성공 모킹
            when(chatRoomOperations.leaveRoom(roomId, sessionInfo))
                    .thenReturn(successResult);

            // When
            roomController.leaveRoom(roomId, headerAccessor);

            // Then
            verify(chatRoomOperations, times(1))
                    .leaveRoom(roomId, sessionInfo);
            verify(chatResponseHandler, times(1))
                    .handleRoomLeaveResult(sessionId, successResult);
            verify(chatResponseHandler, never())
                    .sendErrorResponse(any(), any(), any(), any());
        }
    }

    @Test
    void 방_퇴장_인증_실패_시나리오() {
        // Given
        Long roomId = 1L;
        String sessionId = "session-123";

        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create();
        headerAccessor.setSessionId(sessionId);

        try (MockedStatic<StomSessionUtil> mockedStatic = mockStatic(StomSessionUtil.class)) {
            // 세션 정보 추출 실패 모킹 (null 반환)
            mockedStatic.when(() -> StomSessionUtil.extractSessionInfo(headerAccessor))
                    .thenReturn(null);

            // When
            roomController.leaveRoom(roomId, headerAccessor);

            // Then
            verify(chatRoomOperations, never())
                    .leaveRoom(any(), any());
            verify(chatResponseHandler, times(1))
                    .sendErrorResponse(sessionId, "", "AUTHENTICATION_REQUIRED", "인증된 사용자만 방에서 퇴장할 수 있습니다");
        }
    }

    @Test
    void 방_퇴장_예외_발생_시나리오() {
        // Given
        Long roomId = 1L;
        String sessionId = "session-123";
        Long userId = 10L;
        String nickname = "testUser";

        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create();
        headerAccessor.setSessionId(sessionId);

        WebSocketSessionInfo sessionInfo = new WebSocketSessionInfo(userId, nickname, null);

        try (MockedStatic<StomSessionUtil> mockedStatic = mockStatic(StomSessionUtil.class)) {
            // 세션 정보 추출 모킹
            mockedStatic.when(() -> StomSessionUtil.extractSessionInfo(headerAccessor))
                    .thenReturn(sessionInfo);

            // 방 퇴장 시 예외 발생 모킹
            when(chatRoomOperations.leaveRoom(roomId, sessionInfo))
                    .thenThrow(new RuntimeException("Participant removal failed"));

            // When
            roomController.leaveRoom(roomId, headerAccessor);

            // Then
            verify(chatRoomOperations, times(1))
                    .leaveRoom(roomId, sessionInfo);
            verify(chatResponseHandler, times(1))
                    .sendErrorResponse(sessionId, "", "ROOM_LEAVE_FAILED", "방 퇴장에 실패했습니다: Participant removal failed");
        }
    }
}