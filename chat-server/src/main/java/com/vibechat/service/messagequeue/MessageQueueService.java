package com.vibechat.service.messagequeue;

import org.springframework.data.redis.connection.stream.MapRecord;

/**
 * Redis Streams 기반 메시지 큐 서비스 인터페이스
 *
 * 아키텍처:
 * - Room별 Stream: room:{roomId}:messages
 * - Broadcast 방식으로 모든 연결된 유저에게 실시간 전달
 * - Consumer Group으로 오프라인 유저 처리 (DLQ)
 */
public interface MessageQueueService {

    /**
     * Room에 메시지를 Redis Stream에 추가
     *
     * @param roomId 방 ID
     * @param messageData 메시지 데이터 (JSON)
     * @return Redis Stream Message ID
     */
    String addMessage(Long roomId, Object messageData);

    /**
     * Room 전용 Stream 생성/초기화
     * 유저가 처음 Room에 입장할 때 호출
     *
     * @param roomId 방 ID
     */
    void initializeRoomStream(Long roomId);

    /**
     * Room의 Consumer Group 생성 (오프라인 유저 처리용)
     *
     * @param roomId 방 ID
     * @param groupName Consumer Group 이름
     */
    void createConsumerGroup(Long roomId, String groupName);

    /**
     * 실시간 브로드캐스트를 위한 Stream 리스너 시작
     * WebSocket 연결된 유저들에게 즉시 전달
     *
     * @param roomId 방 ID
     */
    void startStreamListener(Long roomId);

    /**
     * Stream 리스너 정지
     * 마지막 유저가 Room에서 나갈 때 호출
     *
     * @param roomId 방 ID
     */
    void stopStreamListener(Long roomId);

    /**
     * 오프라인 유저를 위한 미처리 메시지 조회
     * DLQ(Dead Letter Queue) 처리용
     *
     * @param roomId 방 ID
     * @param consumerGroup Consumer Group 이름
     * @param consumerName Consumer 이름
     * @return 미처리 메시지 목록
     */
    java.util.List<org.springframework.data.redis.connection.stream.ObjectRecord<String, com.vibechat.dto.StreamMessageDto>> getPendingMessages(Long roomId, String consumerGroup, String consumerName);
}