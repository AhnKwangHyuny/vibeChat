package com.vibechat.repository.user.dto;

import com.vibechat.domain.UserProvider;
import com.vibechat.domain.UserStatus;

import java.time.LocalDateTime;

/**
 * JPQL Constructor Expression을 위한 조회 전용 DTO.
 * Entity 대신 이 DTO로 직접 조회하여 성능을 최적화합니다.
 */
public record UserProfileDto(
    Long userId,
    String nickname,
    String avatarUrl,
    String greeting,
    LocalDateTime joinedAt,
    UserStatus status,
    UserProvider userType
) {
}
