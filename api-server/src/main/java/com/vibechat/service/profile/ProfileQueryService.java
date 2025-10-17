package com.vibechat.service.profile;

import com.vibechat.dto.user.ProfileResponseDto;

/**
 * 프로필 조회 Service
 *
 * 책임: 사용자 프로필 정보 조회
 * - 단일 책임 원칙(SRP): 조회만 담당
 * - 수정/삭제는 별도 Service로 분리
 */
public interface ProfileQueryService {

    /**
     * 내 프로필 조회
     *
     * @param userId 사용자 ID
     * @return 프로필 정보
     * @throws com.vibechat.exception.user.UserNotFoundException 사용자를 찾을 수 없는 경우
     */
    ProfileResponseDto getMyProfile(Long userId);
}
