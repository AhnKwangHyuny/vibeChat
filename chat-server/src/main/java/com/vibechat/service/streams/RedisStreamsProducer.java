package com.vibechat.service.streams;

import com.vibechat.dto.enrichment.EnrichedMessage;

/**
 * Redis Streams 메시지 발행 서비스 인터페이스
 *
 * 책임: Room Stream에 메시지 발행 → Consumer Group이 모든 처리 담당
 * - 실시간 브로드캐스트 (RoomBroadcastConsumer)
 * - MongoDB 저장 (MessageStorageConsumer)
 * - 오프라인 알림 (NotificationConsumer)
 */
public interface RedisStreamsProducer {

    /**
     * 방별 스트림에 메시지 발행
     * Consumer Group에서 브로드캐스트, 저장, 알림을 비동기로 처리
     *
     * @param roomId 방 ID
     * @param message 강화된 메시지
     * @return Redis Streams 레코드 ID
     */
    String sendToRoom(Long roomId, EnrichedMessage message);
}