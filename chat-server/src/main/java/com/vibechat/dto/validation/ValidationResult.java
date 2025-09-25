package com.vibechat.dto.validation;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.ToString;

import java.util.List;

/**
 * 메시지 검증 결과 DTO
 *
 * 검증 성공/실패와 상세 에러 정보를 담는 Value Object
 */
@Getter
@Builder
@ToString
public class ValidationResult {

    /**
     * 검증 성공 여부
     */
    private final boolean valid;

    /**
     * 검증 에러 목록 (실패 시)
     */
    @Singular
    private final List<String> errors;

    /**
     * 성공 결과 생성
     */
    public static ValidationResult success() {
        return ValidationResult.builder()
            .valid(true)
            .build();
    }

    /**
     * 실패 결과 생성 (단일 에러)
     */
    public static ValidationResult failure(String error) {
        return ValidationResult.builder()
            .valid(false)
            .error(error)
            .build();
    }

    /**
     * 실패 결과 생성 (다중 에러)
     */
    public static ValidationResult failure(List<String> errors) {
        return ValidationResult.builder()
            .valid(false)
            .errors(errors)
            .build();
    }
}