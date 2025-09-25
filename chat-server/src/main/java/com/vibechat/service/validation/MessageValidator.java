package com.vibechat.service.validation;

import com.vibechat.dto.SendMessagePayload;
import com.vibechat.dto.validation.ValidationResult;

/**
 * 메시지 검증 서비스 인터페이스
 *
 * 책임: 메시지 입력 검증
 * - XSS 검증
 * - 길이 제한 검증
 * - 미디어 타입 검증
 * - 필수 필드 검증
 */
public interface MessageValidator {

    /**
     * 메시지 페이로드 검증
     *
     * @param payload 검증할 메시지 페이로드
     * @return 검증 결과
     */
    ValidationResult validate(SendMessagePayload payload);
}