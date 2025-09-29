package com.vibechat.websocket.session;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * WebSocket 세션 정보 불변 객체
 *
 * 세션에 저장되는 사용자 정보를 캡슐화
 * SRP: 세션 데이터 표현만 담당
 */
@Getter
@Builder
@ToString
public class WebSocketSessionInfo {

    private final String sessionId;
    private final Long userId;
    private final String nickname;
    private final String avatarUrl;
    private final LocalDateTime connectedAt;

    /**
     * 세션 정보 유효성 검증
     */
    public boolean isValid() {
        return sessionId != null &&
               userId != null && userId > 0 &&
               nickname != null && !nickname.trim().isEmpty();
    }

    /**
     * 익명 사용자 여부 확인
     */
    public boolean isAnonymous() {
        return avatarUrl == null || avatarUrl.trim().isEmpty();
    }

    /**
     * 세션 지속 시간 계산 (분 단위)
     */
    public long getSessionDurationMinutes() {
        return java.time.Duration.between(connectedAt, LocalDateTime.now()).toMinutes();
    }
}