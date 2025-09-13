package com.vibechat.controller;

import com.vibechat.dto.GuestUserCreateRequest;
import com.vibechat.dto.UserResponse;
import com.vibechat.dto.logout.LogoutResultDto;
import com.vibechat.service.auth.AuthService;
import com.vibechat.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    @PostMapping(value = "/users/guest", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserResponse> createGuestUser(@Valid @RequestBody GuestUserCreateRequest request,
                                                        HttpServletRequest httpServletRequest) {
        UserResponse userResponse = userService.createGuestUser(request);

        // Store user info in session
        HttpSession session = httpServletRequest.getSession(true);
        session.setAttribute("userId", userResponse.getUserId().toString());
        session.setAttribute("nickname", userResponse.getNickname());
        session.setAttribute("avatarUrl", userResponse.getAvatarUrl());
        session.setAttribute("provider", "GUEST");

        return ResponseEntity.status(HttpStatus.CREATED).body(userResponse);
    }


}