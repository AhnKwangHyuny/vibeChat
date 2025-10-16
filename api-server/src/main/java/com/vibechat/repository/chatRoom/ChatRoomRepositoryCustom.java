package com.vibechat.repository.chatRoom;

import com.vibechat.domain.ChatRoom;
import com.vibechat.dto.room.RoomListRequest;

import java.util.List;

/**
 * ChatRoom 동적 쿼리 인터페이스
 * 
 * 복잡한 검색/필터/정렬 조건을 처리하기 위한 Custom Repository
 * QueryDSL 또는 Criteria API로 구현
 */
public interface ChatRoomRepositoryCustom {

    /**
     * 방 목록 조회 (커서 기반 페이지네이션 + 동적 필터)
     * 
     * N+1 문제 해결: Fetch Join 적용
     * - roomTags, tag, createdBy를 한 번에 로드
     * 
     * @param request 검색/필터/정렬/페이지네이션 조건
     * @return 조회된 방 목록 (Fetch Join 적용)
     */
    List<ChatRoom> findRoomsWithFilters(RoomListRequest request);

    /**
     * 태그 기반 방 검색 (최적화)
     * 
     * EXISTS 서브쿼리 사용으로 성능 최적화
     * 
     * @param tagNames 검색할 태그 목록 (OR 조건)
     * @param lastId 커서 (이전 페이지 마지막 ID)
     * @param limit 페이지 크기
     * @return 태그를 포함한 방 목록
     */
    List<ChatRoom> findByTagsWithCursor(List<String> tagNames, Long lastId, int limit);

    /**
     * 전체 방 개수 조회 (필터 적용)
     * 
     * 성능상 이유로 별도 API에서만 사용 권장
     * 
     * @param request 필터 조건
     * @return 조건에 맞는 전체 방 개수
     */
    long countRoomsWithFilters(RoomListRequest request);
}

