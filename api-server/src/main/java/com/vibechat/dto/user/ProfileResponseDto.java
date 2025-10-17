package com.vibechat.dto.user;

import java.time.LocalDateTime;

public record ProfileResponseDto(
    String userId,
    String nickname,
    String email,
    String avatarUrl,
    String greeting,
    LocalDateTime joinedAt,
    String status,
    String userType,
    StatsDto stats
) {
    public record StatsDto(
        long totalMessages,
        int roomsJoined
    ) {}
}
