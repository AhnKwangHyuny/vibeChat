package com.vibechat.service.room;

import com.vibechat.dto.RoomCreateRequest;
import com.vibechat.dto.RoomResponse;
import java.util.List;

public interface RoomService {
    RoomResponse createRoom(RoomCreateRequest request, Long userId);
    List<RoomResponse> searchRooms(java.util.List<String> tags);
    RoomResponse getRoomById(Long roomId);
    void joinRoom(Long roomId, String inviteCode, Long userId, String nickname);
}


