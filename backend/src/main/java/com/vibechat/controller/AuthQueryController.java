package com.vibechat.controller;

import com.vibechat.dto.logout.LogoutResultDto;
import com.vibechat.service.auth.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

    /**
     * 사용자 정보 로그인(세션)에서 파싱 (아이디, 프로필 사진, 닉네임)
     * */

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(HttpServletRequest request) {

        Map<String, Object> userInfo = authService.getUserInfoFromSession(request);

        return ResponseEntity.ok(userInfo);
    }

    /**
     * 사용자 로그아웃
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @return 로그아웃 결과
     */
    @PostMapping("/logout")
    public ResponseEntity<LogoutResultDto> logout(HttpServletRequest request,
                                                  HttpServletResponse response) {
        LogoutResultDto result = authService.logout(request, response);

        System.out.println("logout = " + request.getRequestURI());
        return ResponseEntity.ok(result);
    }

    /**
     * 현재 로그인 상태 확인
     * @param request HTTP 요청
     * @return 로그인 상태 정보
     */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkLoginStatus(HttpServletRequest request) {
        boolean isLoggedIn = authService.isLoggedIn(request);
        String userId = authService.getCurrentUserId(request);

        Map<String, Object> responseData = Map.of(
                "isLoggedIn", isLoggedIn,
                "userId", userId != null ? userId : ""
        );

        return ResponseEntity.ok(responseData);
    }
}


