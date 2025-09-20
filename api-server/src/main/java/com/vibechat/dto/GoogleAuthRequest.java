package com.vibechat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoogleAuthRequest {
    
    @NotBlank(message = "Access token is required")
    private String accessToken;
    
    @NotBlank(message = "Provider ID is required")
    private String providerId;
    
    @NotBlank(message = "Nickname is required")
    private String nickname;
    
    private String avatarUrl;
    
    private String email;
}
