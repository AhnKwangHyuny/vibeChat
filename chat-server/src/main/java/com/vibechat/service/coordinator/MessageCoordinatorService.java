package com.vibechat.service.coordinator;

import com.vibechat.dto.SendMessagePayload;
import com.vibechat.dto.coordinator.MessageProcessResult;

/**
 * 메시지 처리 조정 서비스 인터페이스
 *
 * 책임: 메시지 처리 워크플로우 조정 및 관리
 * - 레이트 리미팅 체크
 * - 메시지 검증 조정
 * - 메시지 강화 조정
 * - Redis Streams 발행 조정
 */
public interface MessageCoordinatorService {

    /**
     * 방 메시지 처리 (메인 엔트리포인트)
     *
     * @param roomId 방 ID
     * @param userId 사용자 ID
     * @param payload 메시지 페이로드
     * @return 메시지 처리 결과
     */
    MessageProcessResult processRoomMessage(Long roomId, Long userId, SendMessagePayload payload);
}