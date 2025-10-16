package com.vibechat.service.room.query;

import com.vibechat.dto.room.RoomResponse;

import java.util.Optional;

/**
 * 방 조회 서비스 인터페이스
 * 
 * SRP: 단일 방 조회만 담당 (읽기 전용)
 * DIP: Repository 구현체에 직접 의존하지 않음
 * 
 * 책임:
 * - 단일 방 상세 정보 조회
 * - 방 존재 여부 확인
 */
public interface RoomQueryService {

    /**
     * 방 ID로 상세 정보 조회
     * 
     * @param roomId 방 ID
     * @return 방 상세 정보
     * @throws com.vibechat.exception.RoomNotFoundException 방이 존재하지 않을 때
     */
    RoomResponse getRoomById(Long roomId);

    /**
     * 방 ID로 조회 (Optional)
     * 
     * @param roomId 방 ID
     * @return Optional<RoomResponse>
     */
    Optional<RoomResponse> findRoomById(Long roomId);

    /**
     * 방 존재 여부 확인
     * 
     * @param roomId 방 ID
     * @return true if exists
     */
    boolean existsById(Long roomId);

    /**
     * 초대 코드로 방 조회
     * 
     * 비공개 방 입장 시 사용
     * 
     * @param inviteCode 초대 코드
     * @return 방 정보
     * @throws com.vibechat.exception.InvalidInviteCodeException 유효하지 않은 코드
     */
    RoomResponse getRoomByInviteCode(String inviteCode);
}

