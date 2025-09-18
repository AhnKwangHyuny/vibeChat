package com.vibechat.service.room;

import com.vibechat.dto.room.RoomCreateRequest;
import com.vibechat.dto.room.RoomResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

public interface RoomService {
    RoomResponse createRoom(@Valid @RequestBody RoomCreateRequest roomCreateRequest, HttpServletRequest request);
    List<RoomResponse> searchRooms(List<String> tags);
    RoomResponse getRoomById(Long roomId);
    void joinRoom(Long roomId, String inviteCode, Long userId, String nickname);
}


