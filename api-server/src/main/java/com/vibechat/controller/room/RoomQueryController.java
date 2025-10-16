package com.vibechat.controller.room;

import com.vibechat.dto.room.RoomResponse;
import com.vibechat.service.room.query.RoomQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 방 조회 Controller
 * 
 * 책임: HTTP 요청/응답 처리만 담당 (오케스트레이션)
 * - 비즈니스 로직 없음
 * - Service Layer 호출만 수행
 */
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Slf4j
public class RoomQueryController {

    private final RoomQueryService roomQueryService;

    /**
     * 방 상세 정보 조회
     * 
     * GET /api/rooms/{roomId}
     * 
     * @param roomId 방 ID
     * @return 방 상세 정보
     */
    @GetMapping("/{roomId}")
    public ResponseEntity<RoomResponse> getRoomById(@PathVariable Long roomId) {
        log.info("[RoomQueryController] 방 조회 요청: roomId={}", roomId);

        RoomResponse response = roomQueryService.getRoomById(roomId);

        log.info("[RoomQueryController] 방 조회 완료: roomId={}, title={}", 
            roomId, response.getTitle());

        return ResponseEntity.ok(response);
    }

    /**
     * 방 존재 여부 확인
     * 
     * HEAD /api/rooms/{roomId}
     * 
     * @param roomId 방 ID
     * @return 200 OK if exists, 404 NOT FOUND otherwise
     */
    @RequestMapping(value = "/{roomId}", method = RequestMethod.HEAD)
    public ResponseEntity<Void> checkRoomExists(@PathVariable Long roomId) {
        log.debug("[RoomQueryController] 방 존재 여부 확인: roomId={}", roomId);

        boolean exists = roomQueryService.existsById(roomId);

        if (exists) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 초대 코드로 방 조회 (향후 구현)
     * 
     * GET /api/rooms/invite/{inviteCode}
     * 
     * @param inviteCode 초대 코드
     * @return 방 정보
     */
    @GetMapping("/invite/{inviteCode}")
    public ResponseEntity<RoomResponse> getRoomByInviteCode(@PathVariable String inviteCode) {
        log.info("[RoomQueryController] 초대 코드로 조회: code={}", inviteCode);

        RoomResponse response = roomQueryService.getRoomByInviteCode(inviteCode);

        return ResponseEntity.ok(response);
    }
}

