package com.vibechat.controller;

import com.vibechat.config.auth.AuthUser;
import com.vibechat.domain.auth.UserPrincipal;
import com.vibechat.dto.UserResponse;
import com.vibechat.dto.logout.LogoutResultDto;
import com.vibechat.service.auth.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthQueryController {

    private final AuthService authService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthUser(required = false) UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.ok(null); // 로그인하지 않은 경우, 200 OK와 null body 반환
        }

        // 로그인한 경우, UserPrincipal 정보를 UserResponse DTO로 변환하여 반환
        UserResponse userResponse = new UserResponse();
        userResponse.setUserId(principal.id());
        userResponse.setNickname(principal.nickname());

        // userResponse.setAvatarUrl(principal.avatarUrl());

        return ResponseEntity.ok(userResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<LogoutResultDto> logout(HttpServletRequest request,
                                                  HttpServletResponse response) {
        LogoutResultDto result = authService.logout(request, response);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkLoginStatus(@AuthUser(required = false) UserPrincipal principal) {
        boolean isLoggedIn = (principal != null);
        Map<String, Object> responseData = Map.of(
                "isLoggedIn", isLoggedIn,
                "userId", isLoggedIn ? principal.id().toString() : ""
        );
        return ResponseEntity.ok(responseData);
    }
}


