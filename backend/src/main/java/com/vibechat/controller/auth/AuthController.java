package com.vibechat.controller.auth;

import com.vibechat.config.auth.AuthUserArgumentResolver;
import com.vibechat.domain.auth.UserPrincipal;
import com.vibechat.dto.GuestUserCreateRequest;
import com.vibechat.dto.UserResponse;
import com.vibechat.service.auth.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping(value = "/users/guest")
    public ResponseEntity<UserResponse> createGuestUser(@Valid @RequestBody GuestUserCreateRequest request,
                                                        HttpServletRequest httpServletRequest) {
        // 1. 서비스에 인증 위임 후 UserPrincipal 받기
        UserPrincipal principal = authService.authenticateGuest(request);

        // 2. 컨트롤러가 세션 처리 책임지기
        HttpSession session = httpServletRequest.getSession(true);
        session.setAttribute(AuthUserArgumentResolver.USER_PRINCIPAL_ATTRIBUTE, principal);

        // 3. 클라이언트에 응답 DTO 반환
        UserResponse userResponse = new UserResponse();
        userResponse.setUserId(principal.id());
        userResponse.setNickname(principal.nickname());

        return ResponseEntity.status(HttpStatus.CREATED).body(userResponse);
    }
}
