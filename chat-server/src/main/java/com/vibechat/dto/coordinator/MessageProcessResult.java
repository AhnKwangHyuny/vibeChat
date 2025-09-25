package com.vibechat.dto.coordinator;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * 메시지 처리 결과 DTO
 *
 * 클라이언트 ACK 및 상태 응답을 위한 Value Object
 */
@Getter
@Builder
@ToString
public class MessageProcessResult {

    /**
     * 처리 성공 여부
     */
    private final boolean success;

    /**
     * 생성된 메시지 ID (Redis Streams Record ID)
     */
    private final String messageId;

    /**
     * 클라이언트 임시 ID (ACK용)
     */
    private final String clientTempId;

    /**
     * 에러 메시지 (실패 시)
     */
    private final String errorMessage;

    /**
     * 처리 시간 (밀리초)
     */
    private final long processingTimeMs;

    /**
     * 성공 결과 생성
     */
    public static MessageProcessResult success(String messageId, String clientTempId) {
        return MessageProcessResult.builder()
            .success(true)
            .messageId(messageId)
            .clientTempId(clientTempId)
            .processingTimeMs(System.currentTimeMillis()) // 실제로는 시작시간과 차이 계산
            .build();
    }

    /**
     * 실패 결과 생성
     */
    public static MessageProcessResult failure(String errorMessage) {
        return MessageProcessResult.builder()
            .success(false)
            .errorMessage(errorMessage)
            .processingTimeMs(System.currentTimeMillis())
            .build();
    }
}