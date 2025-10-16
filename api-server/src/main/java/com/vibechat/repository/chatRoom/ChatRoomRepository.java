package com.vibechat.repository.chatRoom;

import com.vibechat.domain.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * ChatRoom Repository
 * 
 * Custom Repository 상속으로 동적 쿼리 지원
 * - ChatRoomRepositoryCustom: 복잡한 검색/필터/정렬 처리
 */
@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long>, ChatRoomRepositoryCustom {

    /**
     * 태그 기반 방 검색 (레거시, N+1 문제 있음)
     * 
     * @deprecated findByTagsWithCursor 사용 권장 (Fetch Join + 커서 페이지네이션)
     */
    @Deprecated
    List<ChatRoom> findByRoomTags_Tag_NameIn(List<String> tagNames);

    /**
     * 모든 방 ID 조회
     */
    @Query("select c.id from ChatRoom c")
    List<Long> findAllIds();
    
    /**
     * 방 목록 조회 (Fetch Join, N+1 해결)
     * 
     * 최신순 정렬, 페이지네이션 없음
     * 대량 데이터 시 성능 이슈 가능 → findRoomsWithFilters 사용 권장
     * 
     * @param limit 조회 제한
     * @return 최신 방 목록
     */
    @Query("SELECT DISTINCT c FROM ChatRoom c " +
           "LEFT JOIN FETCH c.roomTags rt " +
           "LEFT JOIN FETCH rt.tag " +
           "LEFT JOIN FETCH c.createdBy " +
           "ORDER BY c.createdAt DESC, c.id DESC")
    List<ChatRoom> findRecentRoomsWithFetchJoin(int limit);
}

