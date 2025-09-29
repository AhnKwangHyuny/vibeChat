package com.vibechat.service.chat;

import com.vibechat.websocket.session.WebSocketSessionInfo;
import com.vibechat.service.chat.dto.RoomJoinResult;
import com.vibechat.service.chat.dto.RoomLeaveResult;

/**
 * 채팅방 입장/퇴장 비즈니스 로직 인터페이스
 *
 */
public interface ChatRoomOperations {

    /**
     * 방 입장 비즈니스 로직 처리
     *
     * @param roomId 방 ID
     * @param sessionInfo 세션 정보 (사용자 ID, 닉네임 포함)
     * @return RoomJoinResult 방 입장 결과 (성공/실패, 시스템 메시지 포함)
     *
     */
    RoomJoinResult joinRoom(Long roomId, WebSocketSessionInfo sessionInfo);

    /**
     * 방 퇴장 비즈니스 로직 처리
     *
     * @param roomId 방 ID
     * @param sessionInfo 세션 정보 (사용자 ID, 닉네임 포함)
     * @return RoomLeaveResult 방 퇴장 결과 (성공/실패, 시스템 메시지 포함)
     *
     */
    RoomLeaveResult leaveRoom(Long roomId, WebSocketSessionInfo sessionInfo);
}