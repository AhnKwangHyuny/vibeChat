package com.vibechat.websocket.auth;

/**
 * WebSocket 인증 전용 예외
 *
 * 인증 실패 시 구체적인 오류 코드와 메시지 제공
 * OCP: 새로운 에러 코드 확장 가능
 */
public class WebSocketAuthenticationException extends Exception {

    private final ErrorCode errorCode;

    public WebSocketAuthenticationException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public WebSocketAuthenticationException(String message, ErrorCode errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * WebSocket 인증 오류 코드
     */
    public enum ErrorCode {
        MISSING_SESSION("MISSING_SESSION", "WebSocket 세션 ID가 없습니다"),
        MISSING_USER_ID("MISSING_USER_ID", "사용자 ID가 필요합니다"),
        MISSING_NICKNAME("MISSING_NICKNAME", "닉네임이 필요합니다"),
        INVALID_USER_ID("INVALID_USER_ID", "유효하지 않은 사용자 ID입니다"),
        INVALID_NICKNAME("INVALID_NICKNAME", "유효하지 않은 닉네임입니다"),
        INVALID_SESSION_DATA("INVALID_SESSION_DATA", "세션 데이터가 유효하지 않습니다"),
        SESSION_STORAGE_ERROR("SESSION_STORAGE_ERROR", "세션 정보 저장에 실패했습니다"),
        INTERNAL_ERROR("INTERNAL_ERROR", "내부 오류가 발생했습니다");

        private final String code;
        private final String description;

        ErrorCode(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }

    @Override
    public String toString() {
        return String.format("WebSocketAuthenticationException{errorCode=%s, message='%s'}",
            errorCode.getCode(), getMessage());
    }
}