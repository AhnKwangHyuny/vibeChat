package com.vibechat.controller;

import com.vibechat.dto.GoogleAuthRequest;
import com.vibechat.dto.UserResponse;
import com.vibechat.service.user.UserService;
import com.vibechat.service.supabase.SupabaseAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class SocialAuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(SocialAuthController.class);
    
    private final UserService userService;
    private final SupabaseAuthService supabaseAuthService;

    @PostMapping(value = "/google", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserResponse> authenticateWithGoogle(@Valid @RequestBody GoogleAuthRequest request, HttpServletRequest httpServletRequest) {
        try {
            logger.info("Google 인증 시도: providerId={}", request.getProviderId());
            
            // 1. Supabase 토큰 검증
            SupabaseAuthService.SupabaseUserInfo supabaseUser = supabaseAuthService.validateUser(request.getAccessToken());
            
            if (supabaseUser == null) {
                logger.warn("Supabase 토큰 검증 실패: providerId={}", request.getProviderId());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            logger.info("Supabase 토큰 검증 성공: supabaseUserId={}, name={}", supabaseUser.getId(), supabaseUser.getName());
            
            // 2. 사용자 생성 또는 업데이트
            UserResponse userResponse = userService.createOrUpdateGoogleUser(
                request.getProviderId(),
                request.getNickname(),
                request.getAvatarUrl(),
                request.getEmail()
            );
            
            logger.info("Google 사용자 생성/업데이트 완료: userId={}, nickname={}", userResponse.getUserId(), userResponse.getNickname());
            
            // 3. 세션에 사용자 정보 저장
            HttpSession session = httpServletRequest.getSession(true);
            session.setAttribute("userId", userResponse.getUserId());
            session.setAttribute("nickname", userResponse.getNickname());
            session.setAttribute("avatarUrl", userResponse.getAvatarUrl());
            session.setAttribute("provider", "GOOGLE");
            
            logger.info("Google 인증 완료: userId={}, sessionId={}", userResponse.getUserId(), session.getId());
            
            return ResponseEntity.status(HttpStatus.OK).body(userResponse);
            
        } catch (Exception e) {
            logger.error("Google 인증 중 오류 발생: providerId={}", request.getProviderId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // 향후 다른 소셜 로그인 추가 예정
    // @PostMapping("/facebook")
    // @PostMapping("/apple")
    // @PostMapping("/kakao")
    // @PostMapping("/naver")
}
