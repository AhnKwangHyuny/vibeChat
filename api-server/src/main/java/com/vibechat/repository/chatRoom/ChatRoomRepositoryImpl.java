package com.vibechat.repository.chatRoom;

import com.vibechat.domain.ChatRoom;
import com.vibechat.dto.room.RoomListRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * ChatRoomRepositoryCustom 구현체
 * 
 * JPQL 기반 동적 쿼리로 복잡한 검색/필터/정렬 처리
 * 
 * 최적화 포인트:
 * 1. Fetch Join으로 N+1 문제 완전 해결
 * 2. 커서 기반 페이지네이션으로 성능 보장
 * 3. 동적 쿼리로 확장성 확보
 */
@Slf4j
public class ChatRoomRepositoryImpl implements ChatRoomRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<ChatRoom> findRoomsWithFilters(RoomListRequest request) {
        log.debug("[ChatRoomRepository] 방 목록 조회 시작: {}", request);

        // 동적 쿼리 빌드
        StringBuilder jpql = new StringBuilder();
        
        // SELECT with Fetch Join (N+1 해결)
        jpql.append("SELECT DISTINCT c FROM ChatRoom c ");
        jpql.append("LEFT JOIN FETCH c.roomTags rt ");
        jpql.append("LEFT JOIN FETCH rt.tag t ");
        jpql.append("LEFT JOIN FETCH c.createdBy u ");
        
        // WHERE 절 동적 구성
        jpql.append("WHERE 1=1 ");
        
        // 1. 커서 기반 페이지네이션
        if (request.getLastId() != null) {
            if ("DESC".equals(request.getSortOrder())) {
                jpql.append("AND c.id < :lastId ");
            } else {
                jpql.append("AND c.id > :lastId ");
            }
        }
        
        // 2. 검색어 필터 (제목 OR 설명)
        if (request.getQuery() != null && !request.getQuery().isBlank()) {
            jpql.append("AND (LOWER(c.title) LIKE LOWER(:query) ");
            jpql.append("OR LOWER(c.description) LIKE LOWER(:query)) ");
        }
        
        // 3. 공개/비공개 필터
        if (request.getIsPrivate() != null) {
            jpql.append("AND c.isPrivate = :isPrivate ");
        }
        
        // 4. 태그 필터 (EXISTS 서브쿼리로 최적화)
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            jpql.append("AND EXISTS (");
            jpql.append("  SELECT 1 FROM RoomTag rt2 ");
            jpql.append("  WHERE rt2.chatRoom = c ");
            jpql.append("  AND rt2.tag.name IN :tagNames");
            jpql.append(") ");
        }
        
        // ORDER BY (정렬 기준)
        jpql.append("ORDER BY ");
        switch (request.getSortBy()) {
            case "createdAt":
                jpql.append("c.createdAt ").append(request.getSortOrder()).append(", ");
                break;
            case "title":
                jpql.append("c.title ").append(request.getSortOrder()).append(", ");
                break;
            // participantsCount는 향후 구현 (Redis 또는 별도 컬럼)
            default:
                jpql.append("c.createdAt DESC, ");
        }
        jpql.append("c.id ").append(request.getSortOrder());
        
        // 쿼리 생성 및 파라미터 바인딩
        TypedQuery<ChatRoom> query = em.createQuery(jpql.toString(), ChatRoom.class);
        
        if (request.getLastId() != null) {
            query.setParameter("lastId", request.getLastId());
        }
        
        if (request.getQuery() != null && !request.getQuery().isBlank()) {
            query.setParameter("query", "%" + request.getQuery() + "%");
        }
        
        if (request.getIsPrivate() != null) {
            query.setParameter("isPrivate", request.getIsPrivate());
        }
        
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            query.setParameter("tagNames", request.getTags());
        }
        
        // Limit 적용
        query.setMaxResults(request.getLimit());
        
        List<ChatRoom> results = query.getResultList();
        log.debug("[ChatRoomRepository] 방 목록 조회 완료: {} 건", results.size());
        
        return results;
    }

    @Override
    public List<ChatRoom> findByTagsWithCursor(List<String> tagNames, Long lastId, int limit) {
        log.debug("[ChatRoomRepository] 태그 기반 검색: tags={}, lastId={}, limit={}", 
            tagNames, lastId, limit);

        String jpql = "SELECT DISTINCT c FROM ChatRoom c " +
                     "LEFT JOIN FETCH c.roomTags rt " +
                     "LEFT JOIN FETCH rt.tag t " +
                     "LEFT JOIN FETCH c.createdBy u " +
                     "WHERE EXISTS (" +
                     "  SELECT 1 FROM RoomTag rt2 " +
                     "  WHERE rt2.chatRoom = c " +
                     "  AND rt2.tag.name IN :tagNames" +
                     ") ";
        
        if (lastId != null) {
            jpql += "AND c.id < :lastId ";
        }
        
        jpql += "ORDER BY c.createdAt DESC, c.id DESC";
        
        TypedQuery<ChatRoom> query = em.createQuery(jpql, ChatRoom.class);
        query.setParameter("tagNames", tagNames);
        
        if (lastId != null) {
            query.setParameter("lastId", lastId);
        }
        
        query.setMaxResults(limit);
        
        return query.getResultList();
    }

    @Override
    public long countRoomsWithFilters(RoomListRequest request) {
        log.debug("[ChatRoomRepository] 방 개수 조회: {}", request);

        // COUNT 쿼리 (Fetch Join 제외)
        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT COUNT(DISTINCT c) FROM ChatRoom c ");
        
        // 태그 필터 시 JOIN 필요
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            jpql.append("JOIN c.roomTags rt ");
        }
        
        jpql.append("WHERE 1=1 ");
        
        if (request.getQuery() != null && !request.getQuery().isBlank()) {
            jpql.append("AND (LOWER(c.title) LIKE LOWER(:query) ");
            jpql.append("OR LOWER(c.description) LIKE LOWER(:query)) ");
        }
        
        if (request.getIsPrivate() != null) {
            jpql.append("AND c.isPrivate = :isPrivate ");
        }
        
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            jpql.append("AND rt.tag.name IN :tagNames ");
        }
        
        TypedQuery<Long> query = em.createQuery(jpql.toString(), Long.class);
        
        if (request.getQuery() != null && !request.getQuery().isBlank()) {
            query.setParameter("query", "%" + request.getQuery() + "%");
        }
        
        if (request.getIsPrivate() != null) {
            query.setParameter("isPrivate", request.getIsPrivate());
        }
        
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            query.setParameter("tagNames", request.getTags());
        }
        
        return query.getSingleResult();
    }
}

