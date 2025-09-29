package com.vibechat.utils.session;

import com.vibechat.websocket.session.WebSocketSessionInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

import java.util.Map;

/**
 * STOMP over WebSocket 세션 관련 유틸리티 클래스
 */
@Slf4j
public final class StomSessionUtil { // 사용자 요청 이름으로 클래스명 수정

    private StomSessionUtil() {}

    /**
     * STOMP 메시지 헤더의 세션 속성에서 인증된 사용자 정보를 추출합니다.
     *
     * @param headerAccessor STOMP 메시지 헤더 접근자
     * @return 추출된 WebSocketSessionInfo 객체. 정보가 없거나 에러 발생 시 null을 반환합니다.
     */
    public static WebSocketSessionInfo extractSessionInfo(SimpMessageHeaderAccessor headerAccessor) {
        if (headerAccessor == null) {
            return null;
        }
        try {
            Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
            if (sessionAttributes != null) {
                Object sessionInfo = sessionAttributes.get("sessionInfo");
                if (sessionInfo instanceof WebSocketSessionInfo) {
                    return (WebSocketSessionInfo) sessionInfo;
                }
            }
        } catch (Exception e) {
            log.warn("[세션 정보 추출 실패] sessionId={}", headerAccessor.getSessionId(), e);
        }
        return null;
    }
}