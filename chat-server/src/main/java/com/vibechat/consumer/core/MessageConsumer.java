package com.vibechat.consumer.core;

import org.springframework.data.redis.connection.stream.MapRecord;

/**
 * Redis Streams 메시지 컨슈머 기본 인터페이스
 *
 * 모든 Consumer가 구현해야 하는 핵심 계약 정의
 * SRP: 단일 메시지 처리 책임만 가짐
 * OCP: 새로운 Csumer 타입 확장 가능
 */
public interface MessageConsumer {

    /**
     * 메시지 처리 핵심 메소드
     *
     * @param record Redis Streams에서 받은 메시지 레코드 (MapRecord - Redis native 구조)
     * @throws ConsumerProcessingException 처리 실패 시
     */
    void processMessage(MapRecord<String, String, Object> record) throws ConsumerProcessingException;

    /**
     * Consumer 타입 식별자
     *
     * @return Consumer 고유 타입 (로깅, 모니터링용)
     */
    String getConsumerType();

    /**
     * 처리 가능한 메시지 타입 확인
     *
     * @param messageType 메시지 타입
     * @return 처리 가능 여부
     */
    boolean canProcess(String messageType);

    /**
     * Consumer 헬스체크
     *
     * @return 정상 작동 여부
     */
    default boolean isHealthy() {
        return true;
    }
}