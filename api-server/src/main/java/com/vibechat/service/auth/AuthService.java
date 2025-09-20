package com.vibechat.service.auth;

import com.vibechat.domain.auth.UserPrincipal;
import com.vibechat.dto.GuestUserCreateRequest;
import com.vibechat.dto.GoogleAuthRequest;
import com.vibechat.dto.UserResponse;
import com.vibechat.dto.logout.LogoutResultDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {

    UserPrincipal authenticateGuest(GuestUserCreateRequest request);

    UserPrincipal authenticateWithGoogle(GoogleAuthRequest request);

    UserResponse authenticateWithGoogleForResponse(GoogleAuthRequest request);


    LogoutResultDto logout(HttpServletRequest request, HttpServletResponse response);

}