package com.vibechat.service.chat;

import com.vibechat.websocket.session.WebSocketSessionInfo;
import com.vibechat.service.room.RoomParticipantService;
import com.vibechat.service.streams.DynamicStreamOperations;
import com.vibechat.service.chat.dto.RoomJoinResult;
import com.vibechat.service.chat.dto.RoomLeaveResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 채팅방 입장/퇴장 비즈니스 로직 구현체
 *
 * 구현 내용:
 * - Redis Streams 동적 생성 및 Consumer Group 등록
 * - 방 참가자 Redis Set 관리
 * - 시스템 메시지 텍스트 생성
 * - 비즈니스 로직 오류 처리 및 결과 DTO 생성
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatRoomService implements ChatRoomOperations {

    private final RoomParticipantService roomParticipantService;
    private final DynamicStreamOperations dynamicStreamService;

    /**
     * 방 입장 비즈니스 로직 구현
     */
    @Override
    public RoomJoinResult joinRoom(Long roomId, WebSocketSessionInfo sessionInfo) {
        try {
            Long userId = sessionInfo.getUserId();
            String nickname = sessionInfo.getNickname();

            log.info("[방 입장 비즈니스 로직] userId={}, roomId={}, nickname={}",
                userId, roomId, nickname);

            // 1. 동적 스트림 생성 및 Consumer Group 등록
            dynamicStreamService.ensureRoomStream(roomId);
            dynamicStreamService.ensureUserStream(userId);

            // 2. 방 참가자로 등록
            roomParticipantService.addParticipant(roomId, userId);

            // 3. 성공 결과 반환 (시스템 메시지 포함)
            return RoomJoinResult.success(
                roomId,
                userId,
                nickname,
                nickname + "님이 방에 입장했습니다."
            );

        } catch (Exception e) {
            log.error("[방 입장 비즈니스 로직 실패] roomId={}, userId={}",
                roomId, sessionInfo.getUserId(), e);

            return RoomJoinResult.failure(
                roomId,
                sessionInfo.getUserId(),
                sessionInfo.getNickname(),
                "방 입장에 실패했습니다: " + e.getMessage()
            );
        }
    }

    /**
     * 방 퇴장 비즈니스 로직 구현
     */
    @Override
    public RoomLeaveResult leaveRoom(Long roomId, WebSocketSessionInfo sessionInfo) {
        try {
            Long userId = sessionInfo.getUserId();
            String nickname = sessionInfo.getNickname();

            log.info("[방 퇴장 비즈니스 로직] userId={}, roomId={}, nickname={}",
                userId, roomId, nickname);

            // 1. 방 참가자에서 제거
            roomParticipantService.removeParticipant(roomId, userId);

            // 2. 성공 결과 반환 (시스템 메시지 포함)
            return RoomLeaveResult.success(
                roomId,
                userId,
                nickname,
                nickname + "님이 방에서 나갔습니다."
            );

        } catch (Exception e) {
            log.error("[방 퇴장 비즈니스 로직 실패] roomId={}, userId={}",
                roomId, sessionInfo.getUserId(), e);

            return RoomLeaveResult.failure(
                roomId,
                sessionInfo.getUserId(),
                sessionInfo.getNickname(),
                "방 퇴장에 실패했습니다: " + e.getMessage()
            );
        }
    }
}