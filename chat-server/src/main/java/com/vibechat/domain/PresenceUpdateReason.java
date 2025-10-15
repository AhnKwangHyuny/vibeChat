package com.vibechat.domain;

/**
 * Presence 업데이트 사유
 *
 * 방의 참가자 변경 이유를 명시적으로 표현
 */
public enum PresenceUpdateReason {

    /**
     * 사용자 입장
     */
    USER_JOINED("사용자 입장"),

    /**
     * 사용자 퇴장
     */
    USER_LEFT("사용자 퇴장"),

    /**
     * 강제 퇴장
     */
    USER_KICKED("강제 퇴장"),

    /**
     * 연결 끊김
     */
    CONNECTION_LOST("연결 끊김");

    private final String description;

    PresenceUpdateReason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

