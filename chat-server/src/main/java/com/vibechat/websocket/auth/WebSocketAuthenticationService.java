package com.vibechat.websocket.auth;

import com.vibechat.websocket.session.WebSocketSessionInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * WebSocket 인증 전담 서비스
 *
 * SRP: WebSocket 연결 시 사용자 인증만 담당
 * OCP: 새로운 인증 방식 확장 가능
 */
@Service
@Slf4j
public class WebSocketAuthenticationService {

    /**
     * STOMP 헤더에서 사용자 인증 정보 추출 및 검증
     */
    public WebSocketSessionInfo authenticate(StompHeaderAccessor accessor) throws WebSocketAuthenticationException {
        try {
            // 1. 필수 헤더 추출
            String userId = accessor.getFirstNativeHeader("userId");
            String nickname = accessor.getFirstNativeHeader("nickname");
            String avatarUrl = accessor.getFirstNativeHeader("avatarUrl");
            String sessionId = accessor.getSessionId();

            System.out.println("user websocket stompSession: sessionId = " + sessionId);

            // 2. 기본 유효성 검증
            if (sessionId == null) {
                throw new WebSocketAuthenticationException("WebSocket 세션 ID가 없습니다",
                    WebSocketAuthenticationException.ErrorCode.MISSING_SESSION);
            }

            if (userId == null || userId.trim().isEmpty()) {
                throw new WebSocketAuthenticationException("사용자 ID가 필요합니다",
                    WebSocketAuthenticationException.ErrorCode.MISSING_USER_ID);
            }

            if (nickname == null || nickname.trim().isEmpty()) {
                throw new WebSocketAuthenticationException("닉네임이 필요합니다",
                    WebSocketAuthenticationException.ErrorCode.MISSING_NICKNAME);
            }

            // 3. 사용자 ID 형식 검증
            Long parsedUserId;
            try {
                parsedUserId = Long.parseLong(userId);
                if (parsedUserId <= 0) {
                    throw new WebSocketAuthenticationException("유효하지 않은 사용자 ID: " + userId,
                        WebSocketAuthenticationException.ErrorCode.INVALID_USER_ID);
                }
            } catch (NumberFormatException e) {
                throw new WebSocketAuthenticationException("사용자 ID는 숫자여야 합니다: " + userId,
                    WebSocketAuthenticationException.ErrorCode.INVALID_USER_ID);
            }

            // 4. 닉네임 길이 및 형식 검증
            if (nickname.length() > 50) {
                throw new WebSocketAuthenticationException("닉네임은 50자를 초과할 수 없습니다",
                    WebSocketAuthenticationException.ErrorCode.INVALID_NICKNAME);
            }

            // 5. WebSocketSessionInfo 생성
            WebSocketSessionInfo sessionInfo = WebSocketSessionInfo.builder()
                .sessionId(sessionId)
                .userId(parsedUserId)
                .nickname(nickname.trim())
                .avatarUrl(avatarUrl != null ? avatarUrl.trim() : null)
                .connectedAt(LocalDateTime.now())
                .build();

            // 6. 최종 유효성 검증
            if (!sessionInfo.isValid()) {
                throw new WebSocketAuthenticationException("세션 정보가 유효하지 않습니다",
                    WebSocketAuthenticationException.ErrorCode.INVALID_SESSION_DATA);
            }

            log.info("[WebSocket 인증 성공] userId={}, nickname={}, sessionId={}",
                parsedUserId, nickname, sessionId);

            return sessionInfo;

        } catch (WebSocketAuthenticationException e) {
            log.warn("[WebSocket 인증 실패] {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[WebSocket 인증 예외] 예상치 못한 오류 발생", e);
            throw new WebSocketAuthenticationException("인증 처리 중 오류가 발생했습니다",
                WebSocketAuthenticationException.ErrorCode.INTERNAL_ERROR, e);
        }
    }

    /**
     * 세션 속성에 인증 정보 저장
     */
    public void storeSessionInfo(StompHeaderAccessor accessor, WebSocketSessionInfo sessionInfo)
        throws WebSocketAuthenticationException {

        try {
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes == null) {
                throw new WebSocketAuthenticationException("세션 속성을 저장할 수 없습니다",
                    WebSocketAuthenticationException.ErrorCode.SESSION_STORAGE_ERROR);
            }

            // 세션 속성에 정보 저장
            sessionAttributes.put("sessionInfo", sessionInfo);
            sessionAttributes.put("userId", sessionInfo.getUserId());
            sessionAttributes.put("nickname", sessionInfo.getNickname());
            sessionAttributes.put("avatarUrl", sessionInfo.getAvatarUrl());
            sessionAttributes.put("connectedAt", sessionInfo.getConnectedAt());

            log.debug("[세션 정보 저장 완료] sessionId={}, userId={}",
                sessionInfo.getSessionId(), sessionInfo.getUserId());

        } catch (Exception e) {
            log.error("[세션 정보 저장 실패] sessionId={}", sessionInfo.getSessionId(), e);
            throw new WebSocketAuthenticationException("세션 정보 저장에 실패했습니다",
                WebSocketAuthenticationException.ErrorCode.SESSION_STORAGE_ERROR, e);
        }
    }

    /**
     * 세션에서 사용자 정보 추출
     */
    public WebSocketSessionInfo extractSessionInfo(StompHeaderAccessor accessor) {
        try {
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes == null) {
                return null;
            }

            return (WebSocketSessionInfo) sessionAttributes.get("sessionInfo");
        } catch (Exception e) {
            log.warn("[세션 정보 추출 실패] sessionId={}", accessor.getSessionId(), e);
            return null;
        }
    }
}