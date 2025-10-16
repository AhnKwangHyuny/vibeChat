package com.vibechat.service.room.participants;

import java.util.List;
import java.util.Map;

/**
 * 방 참가자 수 조회 서비스 인터페이스
 * 
 * SRP: 참가자 수 조회만 담당 (읽기 전용)
 * DIP: Redis 구현체에 직접 의존하지 않음
 * 
 * 책임:
 * - Redis 기반 실시간 참가자 수 조회
 * - 배치 조회로 성능 최적화
 * 
 * Note: 
 * - chat-server의 RoomParticipantService와 분리
 * - api-server는 조회만, chat-server는 CRUD 담당
 */
public interface RoomParticipantCountService {

    /**
     * 단일 방의 참가자 수 조회
     * 
     * Redis Key: "room:{roomId}:participants"
     * 
     * @param roomId 방 ID
     * @return 현재 참가자 수 (Redis에 없으면 0)
     */
    int getParticipantCount(Long roomId);

    /**
     * 여러 방의 참가자 수 배치 조회 (성능 최적화)
     * 
     * Redis Pipeline 또는 MGET 사용
     * N번 조회 → 1번 조회
     * 
     * @param roomIds 조회할 방 ID 목록
     * @return Map<방 ID, 참가자 수>
     */
    Map<Long, Integer> getParticipantCounts(List<Long> roomIds);

    /**
     * 방 참가자 수가 존재하는지 확인
     * 
     * @param roomId 방 ID
     * @return true if exists in Redis
     */
    boolean hasParticipantData(Long roomId);
}

