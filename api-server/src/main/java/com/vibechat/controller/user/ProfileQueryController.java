package com.vibechat.controller.user;

import com.vibechat.config.AuthUser;
import com.vibechat.domain.auth.UserPrincipal;
import com.vibechat.dto.user.ProfileResponseDto;
import com.vibechat.service.profile.ProfileQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 프로필 조회 Controller
 *
 * 책임: HTTP 요청/응답 처리만 담당 (오케스트레이션)
 * - 비즈니스 로직 없음
 * - Service Layer 호출만 수행
 */
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Slf4j
public class ProfileQueryController {

    private final ProfileQueryService profileQueryService;

    /**
     * 내 프로필 조회
     *
     * GET /api/profile/me
     *
     * @param principal 현재 로그인한 사용자 (SecurityContext에서 추출)
     * @return 프로필 정보
     */
    @GetMapping("/me")
    public ResponseEntity<ProfileResponseDto> getMyProfile(
        @AuthUser(required = true) UserPrincipal principal
    ) {
        log.info("[ProfileQueryController] 프로필 조회 요청: userId={}", principal.id());

        ProfileResponseDto response = profileQueryService.getMyProfile(principal.id());

        log.info("[ProfileQueryController] 프로필 조회 완료: userId={}, nickname={}",
            principal.id(), response.nickname());

        return ResponseEntity.ok(response);
    }
}
