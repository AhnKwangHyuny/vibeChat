package com.vibechat.service.user;

import com.vibechat.dto.GuestUserCreateRequest;
import com.vibechat.dto.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface UserService {
    UserResponse createGuestUser(GuestUserCreateRequest request);
    UserResponse createOrUpdateGoogleUser(String providerId, String nickname, String avatarUrl, String email);
}


