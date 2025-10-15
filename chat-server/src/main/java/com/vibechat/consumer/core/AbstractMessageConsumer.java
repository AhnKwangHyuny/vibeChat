package com.vibechat.consumer.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;

import java.util.Map;
import java.util.Set;

/**
 * 추상 메시지 컨슈머 기반 클래스
 *
 * 템플릿 메소드 패턴을 통한 공통 처리 로직 제공
 * DIP: 구체 구현은 하위 클래스에 위임
 * OCP: 새로운 Consumer 확장 시 기존 코드 수정 없음
 */
@Slf4j
public abstract class AbstractMessageConsumer implements MessageConsumer {

    private final String consumerType;
    private final Set<String> supportedMessageTypes;

    protected AbstractMessageConsumer(String consumerType, Set<String> supportedMessageTypes) {
        this.consumerType = consumerType;
        this.supportedMessageTypes = supportedMessageTypes;
    }

    @Override
    public final void processMessage(MapRecord<String, String, Object> record) throws ConsumerProcessingException {
        String messageId = record.getId().getValue();

        try {
            log.info("[{}] 메시지 처리 시작: messageId={}", consumerType, messageId);

            // 1. 메시지 검증
            validateMessage(record);

            // 2. 메시지 타입 확인
            String messageType = extractMessageType(record);
            if (!canProcess(messageType)) {
                log.warn("[{}] 지원하지 않는 메시지 타입: messageType={}, messageId={}",
                    consumerType, messageType, messageId);
                return;
            }

            // 3. 실제 비즈니스 처리
            long startTime = System.currentTimeMillis();
            processBusinessLogic(record);
            long processingTime = System.currentTimeMillis() - startTime;

            // 4. 성공 로깅
            log.info("[{}] 메시지 처리 완료: messageId={}, processingTime={}ms",
                consumerType, messageId, processingTime);

        } catch (ConsumerProcessingException e) {
            log.error("[{}] 메시지 처리 실패: messageId={}", consumerType, messageId, e);
            throw e;
        } catch (Exception e) {
            log.error("[{}] 예상치 못한 오류: messageId={}", consumerType, messageId, e);
            throw new ConsumerProcessingException(consumerType, messageId,
                "예상치 못한 처리 오류가 발생했습니다", e);
        }
    }

    @Override
    public String getConsumerType() {
        return consumerType;
    }

    @Override
    public boolean canProcess(String messageType) {
        return supportedMessageTypes.contains(messageType);
    }

    /**
     * 메시지 기본 검증
     */
    private void validateMessage(MapRecord<String, String, Object> record) throws ConsumerProcessingException {
        if (record == null) {
            throw new ConsumerProcessingException(consumerType, "null",
                "메시지 레코드가 null입니다", false);
        }

        if (record.getValue() == null) {
            throw new ConsumerProcessingException(consumerType, record.getId().getValue(),
                "메시지 값이 null입니다", false);
        }
    }

    /**
     * 메시지에서 타입 추출
     * 
     * MapRecord 아키텍처:
     * - Redis Streams native hash 구조 사용
     * - MapRecord.getValue()가 직접 Map<String, Object> 반환
     * - 타입 캐스팅 불필요, 간결한 처리
     */
    private String extractMessageType(MapRecord<String, String, Object> record) {
        Map<String, Object> messageData = record.getValue();
        
        String messageType = (String) messageData.get("messageType");
        
        if (messageType == null) {
            log.warn("[{}] messageType 필드 없음: messageId={}, keys={}", 
                consumerType, record.getId().getValue(), messageData.keySet());
            return "UNKNOWN";
        }
        
        return messageType;
    }

    /**
     * 실제 비즈니스 로직 처리 (하위 클래스 구현)
     */
    protected abstract void processBusinessLogic(MapRecord<String, String, Object> record)
        throws ConsumerProcessingException;

    /**
     * 메시지 데이터를 Map으로 안전하게 추출
     * 
     * MapRecord는 이미 Map<String, Object>를 반환하므로 캐스팅 불필요
     */
    protected Map<String, Object> extractMessageData(MapRecord<String, String, Object> record) {
        return record.getValue();
    }

    /**
     * 필수 필드 존재 확인
     */
    protected void requireField(Map<String, Object> messageData, String fieldName, String messageId)
        throws ConsumerProcessingException {
        if (!messageData.containsKey(fieldName) || messageData.get(fieldName) == null) {
            throw new ConsumerProcessingException(consumerType, messageId,
                String.format("필수 필드 누락: %s", fieldName), false);
        }
    }
}