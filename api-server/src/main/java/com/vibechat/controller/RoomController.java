package com.vibechat.controller;

import com.vibechat.config.AuthUser;
import com.vibechat.domain.auth.UserPrincipal;
import com.vibechat.dto.RoomJoinRequest;
import com.vibechat.dto.room.RoomCreateRequest;
import com.vibechat.dto.room.RoomResponse;
import com.vibechat.service.room.RoomService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    /**
     * 방 개설 api
     * */
    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(@Valid @RequestBody RoomCreateRequest roomCreateRequest,
                                                   @AuthUser(required = true) UserPrincipal principal) {

        RoomResponse roomResponse = roomService.createRoom(roomCreateRequest , principal);

        return new ResponseEntity<>(roomResponse, HttpStatus.CREATED);
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<RoomResponse> getRoomById(@PathVariable Long roomId) {

        RoomResponse roomResponse = roomService.getRoomById(roomId);

        return ResponseEntity.ok(roomResponse);
    }

    @PostMapping("/{roomId}/join")
    public ResponseEntity<Void> joinRoom(@PathVariable Long roomId, @RequestBody(required = false) RoomJoinRequest request, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        String inviteCode = (request != null) ? request.getInviteCode() : null;
        String nickname = (request != null) ? request.getNickname() : null;
        if (nickname == null) {
            nickname = (String) session.getAttribute("nickname");
        }
        if (nickname == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST); // Nickname required
        }
        roomService.joinRoom(roomId, inviteCode, userId, nickname);
        return ResponseEntity.ok().build();
    }
}
