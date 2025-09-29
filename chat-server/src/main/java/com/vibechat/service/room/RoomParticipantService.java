package com.vibechat.service.room;

import java.util.List;

/**
 * 방 참가자 관리 서비스 인터페이스
 *
 * 방 참가자의 입장/퇴장 및 목록 관리를 담당
 * DIP: 추상화에 의존하여 구현체 교체 가능
 */
public interface RoomParticipantService {

    /**
     * 방에 참가자 추가
     *
     * @param roomId 방 ID
     * @param userId 사용자 ID
     */
    void addParticipant(Long roomId, Long userId);

    /**
     * 방에서 참가자 제거
     *
     * @param roomId 방 ID
     * @param userId 사용자 ID
     */
    void removeParticipant(Long roomId, Long userId);

    /**
     * 방의 모든 참가자 목록 조회
     *
     * @param roomId 방 ID
     * @return 참가자 ID 목록
     */
    List<Long> getParticipants(Long roomId);

    /**
     * 방의 참가자 수 조회
     *
     * @param roomId 방 ID
     * @return 참가자 수
     */
    int getParticipantCount(Long roomId);

    /**
     * 사용자가 특정 방에 참가 중인지 확인
     *
     * @param roomId 방 ID
     * @param userId 사용자 ID
     * @return 참가 여부
     */
    boolean isParticipant(Long roomId, Long userId);

    /**
     * 사용자가 참가 중인 모든 방 목록 조회
     *
     * @param userId 사용자 ID
     * @return 방 ID 목록
     */
    List<Long> getUserRooms(Long userId);

    /**
     * 방 참가자 목록 정리 (비활성 사용자 제거)
     *
     * @param roomId 방 ID
     */
    void cleanupInactiveParticipants(Long roomId);

    /**
     * 방 참가자 통계 정보 조회
     *
     * @param roomId 방 ID
     * @return 방 참가자 통계 (총 참가자 수, 온라인 참가자 수 등)
     */
    RoomParticipantStats getStats(Long roomId);
}