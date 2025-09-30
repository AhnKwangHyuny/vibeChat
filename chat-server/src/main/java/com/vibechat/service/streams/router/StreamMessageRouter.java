package com.vibechat.service.streams.router;

import org.springframework.data.redis.connection.stream.ObjectRecord;

/**
 * Redis Streams 메시지 라우팅 인터페이스
 *
 * 책임: 스트림 메시지를 적절한 Consumer로 라우팅
 * - 스트림 패턴 기반 메시지 분류
 * - Consumer별 메시지 전달
 * - 라우팅 오류 처리
 */
public interface StreamMessageRouter {

    /**
     * 메시지 라우팅 메인 엔트리포인트
     *
     * @param record Redis Streams 메시지 레코드
     */
    void routeMessage(ObjectRecord<String, Object> record);

    /**
     * 방 메시지 라우팅
     * - RoomBroadcastConsumer (WebSocket 브로드캐스트)
     * - MessageStorageConsumer (MongoDB 저장)
     *
     * @param record 방 스트림 메시지
     */
    void routeRoomMessage(ObjectRecord<String, Object> record);

    /**
     * 사용자 메시지 라우팅
     * - UserReadStateConsumer (읽음 상태 관리)
     * - MessageStorageConsumer (MongoDB 저장)
     * - NotificationConsumer (오프라인 알림)
     *
     * @param record 사용자 스트림 메시지
     */
    void routeUserMessage(ObjectRecord<String, Object> record);

    /**
     * 등록된 스트림 여부 확인
     *
     * @param streamKey 스트림 키
     * @return 등록된 스트림이면 true
     */
    boolean isRegisteredStream(String streamKey);
}