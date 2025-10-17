package com.vibechat.dto.user;

public record ProfileUpdateRequestDto(
    String nickname,
    String greeting
) {}
