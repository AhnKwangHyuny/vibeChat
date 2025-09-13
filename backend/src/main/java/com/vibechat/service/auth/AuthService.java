package com.vibechat.service.auth;

import com.vibechat.dto.logout.LogoutResultDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

/**
 * 인증 관련 서비스 인터페이스
 */
public interface AuthService {

    /**
     * 사용자 로그아웃 처리
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @return 로그아웃 결과
     */
    LogoutResultDto logout(HttpServletRequest request, HttpServletResponse response);

    /**
     * 현재 세션이 유효한 로그인 상태인지 확인
     * @param request HTTP 요청
     * @return 로그인 상태 여부
     */
    boolean isLoggedIn(HttpServletRequest request);

    /**
     * 현재 로그인된 사용자 ID 가져오기
     * @param request HTTP 요청
     * @return 사용자 ID (로그인되지 않은 경우 null)
     */
    String getCurrentUserId(HttpServletRequest request);

    /**
     * 세션에 사용자 정보 저장
     * @param request HTTP 요청
     * @param userId 사용자 ID
     * @param nickname 사용자 닉네임
     * @param avatarUrl 아바타 URL
     * @param provider 로그인 제공자
     */
    void createUserSession(HttpServletRequest request, Long userId, String nickname,
                           String avatarUrl, String provider);

    /**
     * 세션 유효성 검증
     * @param request HTTP 요청
     * @return 세션 유효 여부
     */
    boolean validateSession(HttpServletRequest request);

    /**
     * 게스트 쿠키 헤더(세션)에서 유저 정보 파싱
     * @param request HTTP
     * @retrun 유저 정보 담은 Map 객체
     * */
    Map<String, Object> getUserInfoFromSession(HttpServletRequest request);
}