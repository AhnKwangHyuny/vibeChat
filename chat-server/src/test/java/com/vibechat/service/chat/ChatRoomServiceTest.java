package com.vibechat.service.chat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.vibechat.websocket.session.WebSocketSessionInfo;
import com.vibechat.service.room.RoomParticipantService;
import com.vibechat.service.streams.DynamicStreamOperations;
import com.vibechat.service.chat.dto.RoomJoinResult;
import com.vibechat.service.chat.dto.RoomLeaveResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * ChatRoomService 단위 테스트
 *
 * 테스트 범위:
 * - 방 입장 비즈니스 로직
 * - 방 퇴장 비즈니스 로직
 * - 동적 스트림 생성 통합
 * - 방 참가자 관리 통합
 * - 예외 처리 및 결과 DTO 생성
 */
@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

    @Mock
    private RoomParticipantService roomParticipantService;

    @Mock
    private DynamicStreamOperations dynamicStreamService;

    private ChatRoomService chatRoomService;

    @BeforeEach
    void setUp() {
        chatRoomService = new ChatRoomService(roomParticipantService, dynamicStreamService);
    }

    @Test
    void 방_입장_성공_시나리오() {
        // Given
        Long roomId = 1L;
        Long userId = 10L;
        String nickname = "testUser";
        WebSocketSessionInfo sessionInfo = new WebSocketSessionInfo(userId, nickname, null);

        // 모든 서비스 호출이 성공하도록 모킹
        doNothing().when(dynamicStreamService).ensureRoomStream(roomId);
        doNothing().when(dynamicStreamService).ensureUserStream(userId);
        doNothing().when(roomParticipantService).addParticipant(roomId, userId);

        // When
        RoomJoinResult result = chatRoomService.joinRoom(roomId, sessionInfo);

        // Then
        assertTrue(result.isSuccess());
        assertEquals(roomId, result.getRoomId());
        assertEquals(userId, result.getUserId());
        assertEquals(nickname, result.getNickname());
        assertEquals(nickname + "님이 방에 입장했습니다.", result.getMessage());
        assertNull(result.getErrorMessage());
        assertTrue(result.hasSystemMessage());

        // 서비스 호출 순서 검증
        verify(dynamicStreamService, times(1)).ensureRoomStream(roomId);
        verify(dynamicStreamService, times(1)).ensureUserStream(userId);
        verify(roomParticipantService, times(1)).addParticipant(roomId, userId);
    }

    @Test
    void 방_입장_실패_시나리오_스트림_생성_오류() {
        // Given
        Long roomId = 1L;
        Long userId = 10L;
        String nickname = "testUser";
        WebSocketSessionInfo sessionInfo = new WebSocketSessionInfo(userId, nickname, null);

        // 스트림 생성에서 예외 발생 모킹
        doThrow(new RuntimeException("Redis connection failed"))
                .when(dynamicStreamService).ensureRoomStream(roomId);

        // When
        RoomJoinResult result = chatRoomService.joinRoom(roomId, sessionInfo);

        // Then
        assertFalse(result.isSuccess());
        assertEquals(roomId, result.getRoomId());
        assertEquals(userId, result.getUserId());
        assertEquals(nickname, result.getNickname());
        assertNull(result.getMessage());
        assertEquals("방 입장에 실패했습니다", result.getErrorMessage());
        assertFalse(result.hasSystemMessage());

        // 실패 후 후속 서비스는 호출되지 않음
        verify(dynamicStreamService, times(1)).ensureRoomStream(roomId);
        verify(dynamicStreamService, never()).ensureUserStream(any());
        verify(roomParticipantService, never()).addParticipant(any(), any());
    }

    @Test
    void 방_입장_실패_시나리오_참가자_추가_오류() {
        // Given
        Long roomId = 1L;
        Long userId = 10L;
        String nickname = "testUser";
        WebSocketSessionInfo sessionInfo = new WebSocketSessionInfo(userId, nickname, null);

        // 스트림 생성은 성공하지만 참가자 추가에서 실패
        doNothing().when(dynamicStreamService).ensureRoomStream(roomId);
        doNothing().when(dynamicStreamService).ensureUserStream(userId);
        doThrow(new RuntimeException("Participant limit exceeded"))
                .when(roomParticipantService).addParticipant(roomId, userId);

        // When
        RoomJoinResult result = chatRoomService.joinRoom(roomId, sessionInfo);

        // Then
        assertFalse(result.isSuccess());
        assertEquals("방 입장에 실패했습니다", result.getErrorMessage());

        // 모든 서비스가 호출되었지만 마지막에서 실패
        verify(dynamicStreamService, times(1)).ensureRoomStream(roomId);
        verify(dynamicStreamService, times(1)).ensureUserStream(userId);
        verify(roomParticipantService, times(1)).addParticipant(roomId, userId);
    }

    @Test
    void 방_퇴장_성공_시나리오() {
        // Given
        Long roomId = 1L;
        Long userId = 10L;
        String nickname = "testUser";
        WebSocketSessionInfo sessionInfo = new WebSocketSessionInfo(userId, nickname, null);

        // 참가자 제거가 성공하도록 모킹
        doNothing().when(roomParticipantService).removeParticipant(roomId, userId);

        // When
        RoomLeaveResult result = chatRoomService.leaveRoom(roomId, sessionInfo);

        // Then
        assertTrue(result.isSuccess());
        assertEquals(roomId, result.getRoomId());
        assertEquals(userId, result.getUserId());
        assertEquals(nickname, result.getNickname());
        assertEquals(nickname + "님이 방에서 나갔습니다.", result.getMessage());
        assertNull(result.getErrorMessage());
        assertTrue(result.hasSystemMessage());

        verify(roomParticipantService, times(1)).removeParticipant(roomId, userId);
    }

    @Test
    void 방_퇴장_실패_시나리오() {
        // Given
        Long roomId = 1L;
        Long userId = 10L;
        String nickname = "testUser";
        WebSocketSessionInfo sessionInfo = new WebSocketSessionInfo(userId, nickname, null);

        // 참가자 제거에서 예외 발생 모킹
        doThrow(new RuntimeException("User not found in room"))
                .when(roomParticipantService).removeParticipant(roomId, userId);

        // When
        RoomLeaveResult result = chatRoomService.leaveRoom(roomId, sessionInfo);

        // Then
        assertFalse(result.isSuccess());
        assertEquals(roomId, result.getRoomId());
        assertEquals(userId, result.getUserId());
        assertEquals(nickname, result.getNickname());
        assertNull(result.getMessage());
        assertEquals("방 퇴장에 실패했습니다", result.getErrorMessage());
        assertFalse(result.hasSystemMessage());

        verify(roomParticipantService, times(1)).removeParticipant(roomId, userId);
    }

    @Test
    void 세션_정보가_null인_경우_NullPointerException_발생() {
        // Given
        Long roomId = 1L;
        WebSocketSessionInfo sessionInfo = null;

        // When & Then
        assertThrows(NullPointerException.class, () -> {
            chatRoomService.joinRoom(roomId, sessionInfo);
        });

        assertThrows(NullPointerException.class, () -> {
            chatRoomService.leaveRoom(roomId, sessionInfo);
        });

        // 서비스 호출되지 않음
        verifyNoInteractions(dynamicStreamService);
        verifyNoInteractions(roomParticipantService);
    }
}