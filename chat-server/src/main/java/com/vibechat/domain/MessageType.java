package com.vibechat.domain;

/**
 * 메시지 타입 열거형
 *
 * Chat-Server에서 처리하는 메시지 유형 정의
 */
public enum MessageType {
    TEXT,        // 텍스트 메시지
    IMAGE,       // 이미지 파일
    GIF,         // GIF 이미지
    VIDEO,       // 비디오 파일
    SYSTEM,      // 시스템 메시지 (입장/퇴장 등)
    FILE;        // 일반 파일 (향후 확장)

    /**
     * 문자열로부터 MessageType 변환
     */
    public static MessageType fromString(String value) {
        if (value == null) throw new IllegalArgumentException("message type is null");
        String v = value.trim().toUpperCase();

        try {
            return MessageType.valueOf(v);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown message type: " + value);
        }
    }

    /**
     * 소문자 문자열 반환
     */
    public String toStringValue() {
        return name().toLowerCase();
    }
}