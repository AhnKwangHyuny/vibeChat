package com.vibechat.websocket.connection;

/**
 * WebSocket 연결 처리 전용 예외
 *
 * 연결/해제 이벤트 처리 중 발생하는 오류를 캡슐화
 */
public class WebSocketConnectionException extends RuntimeException {

    public WebSocketConnectionException(String message) {
        super(message);
    }

    public WebSocketConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}