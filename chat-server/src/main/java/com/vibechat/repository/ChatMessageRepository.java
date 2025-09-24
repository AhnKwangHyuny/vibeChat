package com.vibechat.repository;

import com.vibechat.domain.message.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * MongoDB 채팅 메시지 저장소
 * 실시간 채팅 메시지의 영구 저장을 담당 (API-Server 독립적)
 */
@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    /**
     * 방별 최근 메시지 조회
     */
    List<ChatMessage> findTop50ByRoomIdentifierAndStatusIsDeletedFalseOrderByTimestampDesc(String roomIdentifier);

    /**
     * 무한 스크롤을 위한 페이지네이션 조회
     */
    Page<ChatMessage> findByRoomIdentifierAndStatusIsDeletedFalseAndTimestampBeforeOrderByTimestampDesc(
        String roomIdentifier, LocalDateTime before, Pageable pageable);

    /**
     * 특정 시점 이후 메시지 조회 (실시간 동기화용)
     */
    List<ChatMessage> findByRoomIdentifierAndStatusIsDeletedFalseAndTimestampAfterOrderByTimestamp(
        String roomIdentifier, LocalDateTime after);

    /**
     * 사용자별 메시지 조회
     */
    Page<ChatMessage> findByUserIdentifierAndStatusIsDeletedFalseOrderByTimestampDesc(
        String userIdentifier, Pageable pageable);

    /**
     * 클라이언트 임시 ID로 메시지 조회 (ACK 처리용)
     */
    Optional<ChatMessage> findByClientInfoTempId(String tempId);

    /**
     * 메시지 타입별 조회
     */
    List<ChatMessage> findByRoomIdentifierAndTypeAndStatusIsDeletedFalseOrderByTimestampDesc(
        String roomIdentifier, String type);

    /**
     * 읽지 않은 메시지 수 조회
     */
    @Query("{ 'roomIdentifier': ?0, 'status.isDeleted': false, 'delivery.readBy.userId': { $ne: ?1 } }")
    long countUnreadMessages(String roomIdentifier, String userIdentifier);

    /**
     * 특정 유저의 읽음 상태 업데이트
     */
    @Query("{ 'roomIdentifier': ?0, 'status.isDeleted': false, 'delivery.readBy.userId': { $ne: ?1 } }")
    List<ChatMessage> findUnreadMessages(String roomIdentifier, String userIdentifier);

    /**
     * 방별 메시지 수 조회
     */
    long countByRoomIdentifierAndStatusIsDeletedFalse(String roomIdentifier);

    /**
     * 메시지 검색 (텍스트 기반)
     */
    @Query("{ 'roomIdentifier': ?0, 'content.text': { $regex: ?1, $options: 'i' }, 'status.isDeleted': false }")
    List<ChatMessage> searchMessages(String roomIdentifier, String searchText);

    /**
     * 미디어 메시지만 조회
     */
    @Query("{ 'roomIdentifier': ?0, 'type': { $in: ['IMAGE', 'GIF', 'VIDEO', 'FILE'] }, 'status.isDeleted': false }")
    List<ChatMessage> findMediaMessages(String roomIdentifier);

    /**
     * 방별 전체 메시지 삭제 (방 삭제 시 사용)
     */
    void deleteByRoomIdentifier(String roomIdentifier);

    /**
     * 오래된 메시지 정리 (아카이빙)
     */
    @Query("{ 'timestamp': { $lt: ?0 }, 'status.isDeleted': false }")
    List<ChatMessage> findOldMessages(LocalDateTime cutoffDate);
}