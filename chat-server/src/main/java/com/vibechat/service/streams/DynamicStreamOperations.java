package com.vibechat.service.streams;

/**
 * 동적 스트림 관리 서비스 인터페이스
 *
 * DIP(의존성 역전 원칙) 준수를 위한 추상화 계층
 * 새로운 방/사용자 생성 시 Redis Stream을 동적으로 생성하고 관리하는 계약 정의
 */
public interface DynamicStreamOperations {

    /**
     * 방 스트림 존재 확인 및 생성
     *
     * @param roomId 방 ID
     * @throws com.vibechat.exception.streams.DynamicStreamException 스트림 생성 실패 시
     */
    void ensureRoomStream(Long roomId);

    /**
     * 사용자 스트림 존재 확인 및 생성
     *
     * @param userId 사용자 ID
     * @throws com.vibechat.exception.streams.DynamicStreamException 스트림 생성 실패 시
     */
    void ensureUserStream(Long userId);

    /**
     * 방 스트림 정리 (방 삭제 시 사용)
     *
     * @param roomId 방 ID
     */
    void cleanupRoomStream(Long roomId);

    /**
     * 스트림 통계 정보 조회
     *
     * @param streamKey 스트림 키
     * @return 스트림 통계 정보 (메시지 수 등)
     */
    DynamicStreamService.StreamStats getStreamStats(String streamKey);
}