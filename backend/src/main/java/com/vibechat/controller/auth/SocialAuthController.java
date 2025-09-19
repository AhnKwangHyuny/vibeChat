package com.vibechat.controller.auth;

import com.vibechat.config.auth.AuthUserArgumentResolver;
import com.vibechat.domain.UserProvider;
import com.vibechat.domain.auth.UserPrincipal;
import com.vibechat.dto.GoogleAuthRequest;
import com.vibechat.dto.UserResponse;
import com.vibechat.service.auth.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final AuthService authService;


    @PostMapping("/google")
    public ResponseEntity<UserResponse> authenticateWithGoogle(@RequestBody GoogleAuthRequest request, HttpServletRequest httpServletRequest) {
        try {
            // 비즈니스 로직 위임
            UserResponse userResponse = authService.authenticateWithGoogleForResponse(request);

            // 웹 계층 책임: 세션 관리
            HttpSession session = httpServletRequest.getSession(true);
            UserPrincipal principal = new UserPrincipal(
                userResponse.getUserId(),
                userResponse.getNickname(),
                UserProvider.GOOGLE
            );

            session.setAttribute(AuthUserArgumentResolver.USER_PRINCIPAL_ATTRIBUTE, principal);
            session.setAttribute("userId", principal.id());
            session.setAttribute("nickname", principal.nickname());
            session.setAttribute("provider", principal.provider().name());

            return ResponseEntity.ok(userResponse);

        } catch (SecurityException e) {
            logger.warn("Google 인증 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            logger.error("Google 인증 중 예상치 못한 오류 발생: providerId={}", request.getProviderId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
