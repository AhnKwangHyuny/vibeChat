package com.vibechat.controller.room;

import com.vibechat.dto.room.RoomListPageResponse;
import com.vibechat.dto.room.RoomListRequest;
import com.vibechat.service.room.list.RoomListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 방 목록 조회 Controller
 * 
 * 책임: HTTP 요청/응답 처리만 담당 (오케스트레이션)
 * - 비즈니스 로직 없음
 * - Service Layer 호출만 수행
 * - DTO 변환은 Service에서 처리
 */
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Slf4j
public class RoomListController {

    private final RoomListService roomListService;

    /**
     * 방 목록 조회 (메인 API)
     * 
     * GET /api/rooms?query=&tags=&isPrivate=&sortBy=&sortOrder=&limit=&lastId=
     * 
     * @param query 검색어 (선택)
     * @param tags 태그 필터 (선택)
     * @param isPrivate 공개/비공개 필터 (선택)
     * @param sortBy 정렬 기준 (기본값: createdAt)
     * @param sortOrder 정렬 순서 (기본값: DESC)
     * @param limit 페이지 크기 (기본값: 20)
     * @param lastId 커서 (무한 스크롤용, 선택)
     * @return 방 목록 페이지 응답
     */
    @GetMapping
    public ResponseEntity<RoomListPageResponse> getRooms(
        @RequestParam(required = false) String query,
        @RequestParam(required = false) List<String> tags,
        @RequestParam(required = false) Boolean isPrivate,
        @RequestParam(defaultValue = "createdAt") String sortBy,
        @RequestParam(defaultValue = "DESC") String sortOrder,
        @RequestParam(defaultValue = "20") Integer limit,
        @RequestParam(required = false) Long lastId
    ) {
        log.info("[RoomListController] 방 목록 조회 요청: query={}, tags={}, sortBy={}, limit={}", 
            query, tags, sortBy, limit);

        // Request DTO 구성
        RoomListRequest request = RoomListRequest.builder()
            .query(query)
            .tags(tags)
            .isPrivate(isPrivate)
            .sortBy(sortBy)
            .sortOrder(sortOrder)
            .limit(limit)
            .lastId(lastId)
            .build();

        // Service 호출 (비즈니스 로직)
        RoomListPageResponse response = roomListService.getRoomList(request);

        log.info("[RoomListController] 방 목록 조회 완료: {} 건, hasNext={}", 
            response.getSize(), response.isHasNext());

        return ResponseEntity.ok(response);
    }

    /**
     * 태그 기반 방 검색
     * 
     * GET /api/rooms/search?tags=react,typescript&lastId=&limit=
     */
    @GetMapping("/search")
    public ResponseEntity<RoomListPageResponse> searchRoomsByTags(
        @RequestParam List<String> tags,
        @RequestParam(required = false) Long lastId,
        @RequestParam(defaultValue = "20") Integer limit
    ) {
        log.info("[RoomListController] 태그 검색 요청: tags={}", tags);

        RoomListRequest request = RoomListRequest.builder()
            .tags(tags)
            .lastId(lastId)
            .limit(limit)
            .build();

        RoomListPageResponse response = roomListService.getRoomListByTags(request);

        return ResponseEntity.ok(response);
    }

    /**
     * 최신 방 목록
     * 
     * GET /api/rooms/recent?limit=10
     */
    @GetMapping("/recent")
    public ResponseEntity<RoomListPageResponse> getRecentRooms(
        @RequestParam(defaultValue = "20") int limit
    ) {
        log.info("[RoomListController] 최신 방 목록 요청: limit={}", limit);

        RoomListPageResponse response = roomListService.getRecentRooms(limit);

        return ResponseEntity.ok(response);
    }

    /**
     * 인기 방 목록 (향후 구현)
     * 
     * GET /api/rooms/popular?limit=10
     */
    @GetMapping("/popular")
    public ResponseEntity<RoomListPageResponse> getPopularRooms(
        @RequestParam(defaultValue = "20") int limit
    ) {
        log.info("[RoomListController] 인기 방 목록 요청: limit={}", limit);

        RoomListPageResponse response = roomListService.getPopularRooms(limit);

        return ResponseEntity.ok(response);
    }
}

