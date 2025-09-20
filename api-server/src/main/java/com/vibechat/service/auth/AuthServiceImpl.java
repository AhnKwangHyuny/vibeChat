package com.vibechat.service.auth;

import com.vibechat.domain.UserProvider;
import com.vibechat.domain.auth.UserPrincipal;
import com.vibechat.dto.GoogleAuthRequest;
import com.vibechat.dto.GuestUserCreateRequest;
import com.vibechat.dto.UserResponse;
import com.vibechat.dto.logout.LogoutResultDto;
import com.vibechat.service.supabase.SupabaseAuthService;
import com.vibechat.service.user.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final SupabaseAuthService supabaseAuthService;

    @Override
    public UserPrincipal authenticateGuest(GuestUserCreateRequest request) {
        UserResponse userResponse = userService.createGuestUser(request);
        log.info("Guest 사용자 생성 완료: userId={}", userResponse.getUserId());
        return new UserPrincipal(userResponse.getUserId(), userResponse.getNickname(), UserProvider.GUEST);
    }

    @Override
    public UserPrincipal authenticateWithGoogle(GoogleAuthRequest request) {
        log.info("Google 인증 시도: providerId={}", request.getProviderId());

        SupabaseAuthService.SupabaseUserInfo supabaseUser = supabaseAuthService.validateUser(request.getAccessToken());
        if (supabaseUser == null) {
            log.warn("Supabase 토큰 검증 실패: providerId={}", request.getProviderId());
            throw new SecurityException("Invalid access token");
        }
        log.info("Supabase 토큰 검증 성공: supabaseUserId={}", supabaseUser.getId());

        UserResponse userResponse = userService.createOrUpdateGoogleUser(
            request.getProviderId(),
            request.getNickname(),
            request.getAvatarUrl(),
            request.getEmail()
        );
        log.info("Google 사용자 생성/업데이트 완료: userId={}", userResponse.getUserId());

        return new UserPrincipal(userResponse.getUserId(), userResponse.getNickname(), UserProvider.GOOGLE);
    }

    @Override
    public UserResponse authenticateWithGoogleForResponse(GoogleAuthRequest request) {
        log.info("Google 인증 시도: providerId={}", request.getProviderId());

        // Supabase 토큰 검증
        SupabaseAuthService.SupabaseUserInfo supabaseUser = supabaseAuthService.validateUser(request.getAccessToken());
        if (supabaseUser == null) {
            log.warn("Supabase 토큰 검증 실패: providerId={}", request.getProviderId());
            throw new SecurityException("Invalid access token");
        }
        log.info("Supabase 토큰 검증 성공: supabaseUserId={}", supabaseUser.getId());

        // 사용자 생성/업데이트 및 응답 DTO 반환
        UserResponse userResponse = userService.createOrUpdateGoogleUser(
            request.getProviderId(),
            request.getNickname(),
            request.getAvatarUrl(),
            request.getEmail()
        );
        log.info("Google 사용자 생성/업데이트 완료: userId={}", userResponse.getUserId());

        return userResponse;
    }

    @Override
    public LogoutResultDto logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
                clearSessionCookie(response);
                log.info("세션 무효화 및 쿠키 삭제 완료.");
            }
            return LogoutResultDto.success(null);
        } catch (Exception e) {
            log.error("Unexpected error during logout: {}", e.getMessage(), e);
            return LogoutResultDto.error("Logout process failed");
        }
    }

    private void clearSessionCookie(HttpServletResponse response) {
        Cookie sessionCookie = new Cookie("SESSION", "");
        sessionCookie.setMaxAge(0);
        sessionCookie.setPath("/");
        response.addCookie(sessionCookie);
        log.debug("세션 쿠키 삭제 완료");
    }

}
