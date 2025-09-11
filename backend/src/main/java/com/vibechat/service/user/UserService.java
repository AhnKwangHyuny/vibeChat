package com.vibechat.service.user;

import com.vibechat.dto.GuestUserCreateRequest;
import com.vibechat.dto.UserResponse;

public interface UserService {
    UserResponse createGuestUser(GuestUserCreateRequest request);
    UserResponse createOrUpdateGoogleUser(String providerId, String nickname, String avatarUrl, String email);
}


