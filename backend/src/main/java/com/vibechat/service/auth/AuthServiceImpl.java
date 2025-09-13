package com.vibechat.service.auth;

import com.vibechat.dto.logout.LogoutResultDto;
import com.vibechat.exception.auth.UserNotLoggedInException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    /**
     * 사용자 로그아웃 처리
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @return 로그아웃 결과
     */
    @Override
    public LogoutResultDto logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            HttpSession session = request.getSession(false);

            // 세션 존재 여부 확인
            if (session == null) {
                log.warn("Logout attempted but no active session found");
                return LogoutResultDto.noActiveSession();
            }

            // 사용자 정보 가져오기 (로깅용)
            String userId = (String) session.getAttribute("userId");
            String nickname = (String) session.getAttribute("nickname");
            String provider = (String) session.getAttribute("provider");

            if (userId == null) {
                log.warn("Logout attempted but no user info in session");
                return LogoutResultDto.notLoggedIn();
            }

            log.info("User logout initiated - userId: {}, nickname: {}, provider: {}",
                    userId, nickname, provider);

            // 세션 무효화
            session.invalidate();

            // 세션 쿠키 삭제
            clearSessionCookie(response);

            log.info("User successfully logged out - userId: {}", userId);
            return LogoutResultDto.success(userId);

        } catch (IllegalStateException e) {
            log.warn("Logout attempted on already invalidated session: {}", e.getMessage());
            return LogoutResultDto.alreadyLoggedOut();

        } catch (Exception e) {
            log.error("Unexpected error during logout: {}", e.getMessage(), e);
            return LogoutResultDto.error("Logout process failed");
        }
    }

    /**
     * 현재 세션이 유효한 로그인 상태인지 확인
     * @param request HTTP 요청
     * @return 로그인 상태 여부
     */
    @Override
    public boolean isLoggedIn(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null && session.getAttribute("userId") != null;
    }

    /**
     * 현재 로그인된 사용자 ID 가져오기
     * @param request HTTP 요청
     * @return 사용자 ID (로그인되지 않은 경우 null)
     */
    @Override
    public String getCurrentUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            return (String) session.getAttribute("userId");
        }
        return null;
    }

    /**
     * 세션에 사용자 정보 저장
     * @param request HTTP 요청
     * @param userId 사용자 ID
     * @param nickname 사용자 닉네임
     * @param avatarUrl 아바타 URL
     * @param provider 로그인 제공자
     */
    @Override
    public void createUserSession(HttpServletRequest request, Long userId, String nickname,
                                  String avatarUrl, String provider) {
        HttpSession session = request.getSession(true);
        session.setAttribute("userId", userId.toString());
        session.setAttribute("nickname", nickname);
        session.setAttribute("avatarUrl", avatarUrl);
        session.setAttribute("provider", provider);

        // 세션 타임아웃 설정 (30분)
        session.setMaxInactiveInterval(30 * 60);

        log.info("Created session for user - userId: {}, nickname: {}, provider: {}",
                userId, nickname, provider);
    }

    /**
     * 세션 유효성 검증
     * @param request HTTP 요청
     * @return 세션 유효 여부
     */
    @Override
    public boolean validateSession(HttpServletRequest request) {
        try {
            HttpSession session = request.getSession(false);
            if (session == null) {
                return false;
            }

            String userId = (String) session.getAttribute("userId");
            String provider = (String) session.getAttribute("provider");

            if (userId == null || provider == null) {
                log.warn("Invalid session detected - missing required attributes");
                // 잘못된 세션은 무효화
                session.invalidate();
                return false;
            }

            return true;
        } catch (IllegalStateException e) {
            log.warn("Session validation failed - session already invalidated: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Unexpected error during session validation: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 세션 쿠키 삭제
     * @param response HTTP 응답
     */
    private void clearSessionCookie(HttpServletResponse response) {
        Cookie sessionCookie = new Cookie("JSESSIONID", "");
        sessionCookie.setMaxAge(0);
        sessionCookie.setPath("/");
        sessionCookie.setHttpOnly(true);
        // HTTPS 환경에서는 setSecure(true) 설정
        // sessionCookie.setSecure(true);
        response.addCookie(sessionCookie);

        log.debug("세션 쿠키 삭제 완료");
    }

    /**
     * 세션에서 유저 정보 파싱
     * @param request HTTP 요청
     * */

    @Override
    public Map<String, Object> getUserInfoFromSession(HttpServletRequest request) {

        HttpSession session = request.getSession(false); // 세션이 없으면 새로 생성하지 않음

        // 세션이 null이거나 사용자 ID가 없으면 예외를 발생시킵니다.
        if (session == null || session.getAttribute("userId") == null) {
            log.warn("getUserInfoFromSession 실패: 활성 세션 또는 사용자 ID 없음");
            throw new UserNotLoggedInException("세션에 사용자 정보가 없습니다.");
        }

        Map<String, Object> userInfo = Map.of(
                "userId", Long.parseLong((String) session.getAttribute("userId")),
                "nickname", session.getAttribute("nickname"),
                "avatarUrl", session.getAttribute("avatarUrl") != null ? session.getAttribute("avatarUrl") : "",
                "provider", session.getAttribute("provider")
        );

        return userInfo;
    }
}
