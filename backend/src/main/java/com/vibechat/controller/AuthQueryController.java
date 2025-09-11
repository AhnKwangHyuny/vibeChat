package com.vibechat.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthQueryController {

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        String nickname = (String) session.getAttribute("nickname");
        String provider = (String) session.getAttribute("provider");
        String avatarUrl = (String) session.getAttribute("avatarUrl");
        if (userId == null || nickname == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // provider/avatarUrl 포함 반환
        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "nickname", nickname,
                "provider", provider != null ? provider : "GUEST",
                "avatarUrl", avatarUrl
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.noContent().build();
    }
}


