package com.vibechat.service.profile;

import com.vibechat.domain.User;
import com.vibechat.domain.UserProvider;
import com.vibechat.dto.user.ProfileResponseDto;
import com.vibechat.dto.user.ProfileResponseDto.StatsDto;
import com.vibechat.exception.user.UserNotFoundException;
import com.vibechat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 프로필 조회
 *
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProfileQueryServiceImpl implements ProfileQueryService {

    private static final String GUEST_EMAIL = "guest@gmail.com";

    private final UserRepository userRepository;

    @Override
    public ProfileResponseDto getMyProfile(Long userId) {
        log.info("[ProfileQueryService] 프로필 조회 시작: userId={}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다. userId=" + userId));

        ProfileResponseDto response = convertToDto(user);

        log.info("[ProfileQueryService] 프로필 조회 완료: userId={}, nickname={}, provider={}, email={}",
            userId, user.getNickname(), user.getProvider(), response.email());

        return response;
    }

    /**
     * User 엔티티 → ProfileResponseDto 변환
     *
     * SRP: 변환 로직을 별도 메서드로 분리
     */
    private ProfileResponseDto convertToDto(User user) {
        return new ProfileResponseDto(
            user.getId().toString(),
            user.getNickname(),
            resolveEmail(user),
            resolveAvatarUrl(user),
            user.getGreeting(),
            user.getCreatedAt(),
            "online", // TODO: 실제 온라인 상태는 Redis에서 조회 (향후 구현)
            user.getProvider().name(),
            buildStats(user)
        );
    }

    /**
     * Email 해석
     *
     * - GOOGLE: providerId (Google email)
     * - GUEST: "guest@gmail.com"
     */
    private String resolveEmail(User user) {
        if (user.getProvider() == UserProvider.GOOGLE) {
            return user.getProviderId(); // Google의 경우 providerId가 email
        }
        return GUEST_EMAIL;
    }

    /**
     * Avatar URL 해석
     *
     * - 실무 패턴: null이면 프론트엔드에서 처리
     * - avatarUrl이 있으면 그대로 반환 (Google OAuth 또는 업로드된 이미지)
     * - 없으면 null 반환 → 프론트엔드가 기본 이미지 렌더링
     */
    private String resolveAvatarUrl(User user) {
        return (user.getAvatarUrl() != null && !user.getAvatarUrl().isBlank())
            ? user.getAvatarUrl()
            : null;
    }

    /**
     * 통계 정보 생성
     *
     * 현재는 기본값(0) 반환
     */
    private StatsDto buildStats(User user) {
        // 테스트 단계: 기본값 반환
        return new StatsDto(0L, 0);
    }
}
