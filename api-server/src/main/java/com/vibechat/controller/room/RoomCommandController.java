package com.vibechat.controller.room;

import com.vibechat.config.AuthUser;
import com.vibechat.domain.auth.UserPrincipal;
import com.vibechat.dto.RoomJoinRequest;
import com.vibechat.dto.room.RoomCreateRequest;
import com.vibechat.dto.room.RoomResponse;
import com.vibechat.service.room.command.RoomCommandService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 방 생성/수정/삭제 Controller
 * 
 * 책임: HTTP 요청/응답 처리만 담당 (오케스트레이션)
 * - 비즈니스 로직 없음
 * - Service Layer 호출만 수행
 * - 인증 검증은 @AuthUser로 처리
 */
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Slf4j
public class RoomCommandController {

    private final RoomCommandService roomCommandService;

    /**
     * 방 생성
     * 
     * POST /api/rooms
     * 
     * @param request 방 생성 요청 정보
     * @param principal 인증된 사용자 정보
     * @return 생성된 방 정보 (201 Created)
     */
    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(
        @Valid @RequestBody RoomCreateRequest request,
        @AuthUser(required = true) UserPrincipal principal
    ) {
        log.info("[RoomCommandController] 방 생성 요청: title={}, userId={}", 
            request.getTitle(), principal.id());

        RoomResponse response = roomCommandService.createRoom(request, principal);

        log.info("[RoomCommandController] 방 생성 완료: roomId={}", response.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 방 정보 수정
     * 
     * PUT /api/rooms/{roomId}
     * 
     * @param roomId 방 ID
     * @param request 수정 요청 정보
     * @param principal 인증된 사용자 정보
     * @return 수정된 방 정보
     */
    @PutMapping("/{roomId}")
    public ResponseEntity<RoomResponse> updateRoom(
        @PathVariable Long roomId,
        @Valid @RequestBody RoomCreateRequest request,
        @AuthUser(required = true) UserPrincipal principal
    ) {
        log.info("[RoomCommandController] 방 수정 요청: roomId={}, userId={}", 
            roomId, principal.id());

        RoomResponse response = roomCommandService.updateRoom(roomId, request, principal);

        log.info("[RoomCommandController] 방 수정 완료: roomId={}", roomId);

        return ResponseEntity.ok(response);
    }

    /**
     * 방 삭제
     * 
     * DELETE /api/rooms/{roomId}
     * 
     * @param roomId 방 ID
     * @param principal 인증된 사용자 정보
     * @return 204 No Content
     */
    @DeleteMapping("/{roomId}")
    public ResponseEntity<Void> deleteRoom(
        @PathVariable Long roomId,
        @AuthUser(required = true) UserPrincipal principal
    ) {
        log.info("[RoomCommandController] 방 삭제 요청: roomId={}, userId={}", 
            roomId, principal.id());

        roomCommandService.deleteRoom(roomId, principal);

        log.info("[RoomCommandController] 방 삭제 완료: roomId={}", roomId);

        return ResponseEntity.noContent().build();
    }

    /**
     * 방 참여 (기존 레거시 호환: HttpSession 지원)
     * 
     * POST /api/rooms/{roomId}/join
     * 
     * 인증 우선순위:
     * 1. @AuthUser (Principal) - 우선 사용
     * 2. HttpSession - Principal 없을 시 Fallback
     * 
     * @param roomId 방 ID
     * @param request 참여 요청 정보 (초대 코드, 닉네임)
     * @param principal 인증된 사용자 정보 (선택적)
     * @param session HTTP 세션 (Fallback)
     * @return 200 OK
     */
    @PostMapping("/{roomId}/join")
    public ResponseEntity<Void> joinRoom(
        @PathVariable Long roomId,
        @RequestBody(required = false) RoomJoinRequest request,
        @AuthUser(required = false) UserPrincipal principal,
        HttpSession session
    ) {
        // 1. 인증 정보 추출 (Principal 우선, Session Fallback)
        Long userId;
        String nickname;

        if (principal != null) {
            // @AuthUser 기반 인증 (최신 방식)
            userId = principal.id();
            nickname = (request != null && request.getNickname() != null) 
                ? request.getNickname() 
                : principal.nickname();
            
            log.info("[RoomCommandController] 방 참여 요청 (Principal): roomId={}, userId={}, nickname={}", 
                roomId, userId, nickname);
        } else {
            // HttpSession 기반 인증 (레거시 호환)
            userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                log.warn("[RoomCommandController] 인증 실패: 세션 없음");
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }

            nickname = (request != null) ? request.getNickname() : null;
            if (nickname == null) {
                nickname = (String) session.getAttribute("nickname");
            }
            if (nickname == null) {
                log.warn("[RoomCommandController] 닉네임 없음: userId={}", userId);
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            log.info("[RoomCommandController] 방 참여 요청 (Session): roomId={}, userId={}, nickname={}", 
                roomId, userId, nickname);
        }

        // 2. 초대 코드 추출
        String inviteCode = (request != null) ? request.getInviteCode() : null;

        // 3. Service 호출
        roomCommandService.joinRoom(roomId, inviteCode, userId, nickname);

        log.info("[RoomCommandController] 방 참여 완료: roomId={}, userId={}", roomId, userId);

        return ResponseEntity.ok().build();
    }

    /**
     * 방 나가기
     * 
     * POST /api/rooms/{roomId}/leave
     * 
     * @param roomId 방 ID
     * @param principal 인증된 사용자 정보
     * @return 200 OK
     */
    @PostMapping("/{roomId}/leave")
    public ResponseEntity<Void> leaveRoom(
        @PathVariable Long roomId,
        @AuthUser(required = true) UserPrincipal principal
    ) {
        log.info("[RoomCommandController] 방 나가기 요청: roomId={}, userId={}", 
            roomId, principal.id());

        roomCommandService.leaveRoom(roomId, principal.id());

        log.info("[RoomCommandController] 방 나가기 완료: roomId={}, userId={}", 
            roomId, principal.id());

        return ResponseEntity.ok().build();
    }
}

