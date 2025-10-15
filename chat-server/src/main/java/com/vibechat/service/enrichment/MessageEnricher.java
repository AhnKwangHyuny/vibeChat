package com.vibechat.service.enrichment;

import com.vibechat.dto.SendMessagePayload;
import com.vibechat.dto.enrichment.EnrichedMessage;

/**
 * 메시지 강화 서비스 인터페이스
 *
 * 책임: 메시지에 메타데이터 추가
 * - 타임스탬프 추가
 * - 사용자 정보 추가
 * - 방 정보 추가
 * - 메시지 ID 생성
 */
public interface MessageEnricher {

    /**
     * 메시지 페이로드를 강화하여 완전한 메시지 생성
     *
     * @param payload 원본 메시지 페이로드
     * @param userId 사용자 ID
     * @param roomId 방 ID
     * @return 강화된 메시지
     * @deprecated 사용자 정보 누락. enrichWithUserInfo 사용 권장
     */
    @Deprecated
    EnrichedMessage enrich(SendMessagePayload payload, Long userId, Long roomId);

    /**
     * 메시지 페이로드를 사용자 정보와 함께 강화
     *
     * @param payload 원본 메시지 페이로드
     * @param userId 사용자 ID
     * @param roomId 방 ID
     * @param nickname 사용자 닉네임
     * @param avatarUrl 사용자 아바타 URL
     * @return 강화된 메시지 (사용자 정보 포함)
     */
    EnrichedMessage enrichWithUserInfo(SendMessagePayload payload, Long userId, Long roomId, String nickname, String avatarUrl);
}