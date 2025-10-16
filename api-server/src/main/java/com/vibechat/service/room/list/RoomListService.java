package com.vibechat.service.room.list;

import com.vibechat.dto.room.RoomListPageResponse;
import com.vibechat.dto.room.RoomListRequest;

/**
 * 방 목록 조회 서비스 인터페이스
 * 
 * SRP: 방 목록 조회 및 페이지네이션만 담당
 * DIP: 구현체에 의존하지 않고 인터페이스에 의존
 * 
 * 책임:
 * - 방 목록 조회 (필터링, 검색, 정렬)
 * - 커서 기반 페이지네이션
 * - 참가자 수 집계
 */
public interface RoomListService {

    /**
     * 방 목록 조회 (메인 API)
     * 
     * 기능:
     * 1. 동적 필터/검색/정렬 적용
     * 2. 커서 기반 페이지네이션
     * 3. 참가자 수 실시간 조회 (Redis)
     * 4. N+1 문제 해결 (Fetch Join)
     * 
     * @param request 검색/필터/정렬/페이지네이션 조건
     * @return 페이지네이션 응답 (방 목록 + 메타데이터)
     */
    RoomListPageResponse getRoomList(RoomListRequest request);

    /**
     * 태그 기반 방 검색
     * 
     * 특화된 태그 검색 로직
     * - EXISTS 서브쿼리 최적화
     * - 커서 페이지네이션 지원
     * 
     * @param request 태그 필터 포함 요청
     * @return 태그에 해당하는 방 목록
     */
    RoomListPageResponse getRoomListByTags(RoomListRequest request);

    /**
     * 인기 방 목록 조회 (향후 구현)
     * 
     * 참가자 수 기준 정렬
     * - Redis 기반 실시간 카운트
     * - 캐싱 전략 적용
     * 
     * @param limit 조회 개수
     * @return 인기 방 목록
     */
    RoomListPageResponse getPopularRooms(int limit);

    /**
     * 최근 생성된 방 목록
     * 
     * 간단한 최신순 조회 (필터 없음)
     * 
     * @param limit 조회 개수
     * @return 최신 방 목록
     */
    RoomListPageResponse getRecentRooms(int limit);
}

