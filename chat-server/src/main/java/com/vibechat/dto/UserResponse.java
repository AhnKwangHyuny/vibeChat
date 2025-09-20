package com.vibechat.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserResponse {
    private Long userId;
    private String nickname;
    private String avatarUrl;
}
