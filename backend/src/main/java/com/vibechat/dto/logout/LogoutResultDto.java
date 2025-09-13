package com.vibechat.dto.logout;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LogoutResultDto {

    private boolean success;
    private String message;
    private String userId;
    private LogoutStatus status;

    // 팩토리 메서드들
    public static LogoutResultDto success(String userId) {
        return new LogoutResultDto(true, "로그아웃이 성공적으로 완료되었습니다", userId, LogoutStatus.SUCCESS);
    }

    public static LogoutResultDto noActiveSession() {
        return new LogoutResultDto(false, "활성 세션이 없습니다", null, LogoutStatus.NO_SESSION);
    }

    public static LogoutResultDto notLoggedIn() {
        return new LogoutResultDto(false, "로그인되지 않은 사용자입니다", null, LogoutStatus.NOT_LOGGED_IN);
    }

    public static LogoutResultDto alreadyLoggedOut() {
        return new LogoutResultDto(true, "이미 로그아웃된 상태입니다", null, LogoutStatus.ALREADY_LOGGED_OUT);
    }

    public static LogoutResultDto error(String message) {
        return new LogoutResultDto(false, message, null, LogoutStatus.ERROR);
    }
}