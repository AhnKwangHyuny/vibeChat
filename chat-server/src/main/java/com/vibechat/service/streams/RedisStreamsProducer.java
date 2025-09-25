package com.vibechat.service.streams;

import com.vibechat.dto.enrichment.EnrichedMessage;

/**
 * Redis Streams 메시지 발행 서비스 인터페이스
 *
 * 책임: Redis Streams에 메시지 발행
 * - 방별 스트림에 메시지 발행
 * - 사용자별 스트림에 메시지 발행
 * - 오프라인 알림용 DLQ 발행
 */
public interface RedisStreamsProducer {

    /**
     * 방별 스트림에 메시지 발행
     *
     * @param roomId 방 ID
     * @param message 강화된 메시지
     * @return Redis Streams 레코드 ID
     */
    String sendToRoom(Long roomId, EnrichedMessage message);

    /**
     * 사용자별 스트림에 메시지 발행 (읽음 상태 관리용)
     *
     * @param userId 사용자 ID
     * @param message 강화된 메시지
     * @return Redis Streams 레코드 ID
     */
    String sendToUser(Long userId, EnrichedMessage message);

    /**
     * 오프라인 알림용 DLQ에 메시지 발행
     *
     * @param message 강화된 메시지
     * @return Redis Streams 레코드 ID
     */
    String sendToNotificationQueue(EnrichedMessage message);
}