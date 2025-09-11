package com.vibechat.controller;

import com.vibechat.dto.GuestUserCreateRequest;
import com.vibechat.dto.UserResponse;
import com.vibechat.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping(value = "/guest", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserResponse> createGuestUser(@Valid @RequestBody GuestUserCreateRequest request, HttpServletRequest httpServletRequest) {
        UserResponse userResponse = userService.createGuestUser(request);

        // Store user info in session
        HttpSession session = httpServletRequest.getSession(true);
        session.setAttribute("userId", userResponse.getUserId());
        session.setAttribute("nickname", userResponse.getNickname());
        session.setAttribute("avatarUrl", userResponse.getAvatarUrl());
        session.setAttribute("provider", "GUEST");

        return ResponseEntity.status(HttpStatus.CREATED).body(userResponse);
    }
}
