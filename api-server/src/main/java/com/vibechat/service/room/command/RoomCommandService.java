package com.vibechat.service.room.command;

import com.vibechat.domain.auth.UserPrincipal;
import com.vibechat.dto.room.RoomCreateRequest;
import com.vibechat.dto.room.RoomResponse;

/**
 * 방 생성/수정/삭제 서비스 인터페이스
 * 
 * SRP: 방 변경 작업만 담당 (쓰기 전용)
 * DIP: Repository 구현체에 직접 의존하지 않음
 * 
 * 책임:
 * - 방 생성 (태그 할당 포함)
 * - 방 정보 수정
 * - 방 삭제
 * 
 * Note:
 * - 조회 기능은 RoomQueryService 사용
 * - 트랜잭션 관리 필수
 */
public interface RoomCommandService {

    /**
     * 새 방 생성
     * 
     * 프로세스:
     * 1. 방 기본 정보 저장
     * 2. 비공개 방인 경우 초대 코드 생성
     * 3. 태그 할당
     * 
     * @param request 방 생성 요청 정보
     * @param principal 인증된 사용자 정보
     * @return 생성된 방 정보
     */
    RoomResponse createRoom(RoomCreateRequest request, UserPrincipal principal);

    /**
     * 방 정보 수정
     * 
     * 수정 가능 항목:
     * - 제목, 설명
     * - 공개/비공개 전환
     * - 태그
     * 
     * @param roomId 방 ID
     * @param request 수정 요청 정보
     * @param principal 인증된 사용자 정보
     * @return 수정된 방 정보
     * @throws com.vibechat.exception.UnauthorizedException 권한 없음
     */
    RoomResponse updateRoom(Long roomId, RoomCreateRequest request, UserPrincipal principal);

    /**
     * 방 삭제
     * 
     * 프로세스:
     * 1. 권한 확인 (방장만 가능)
     * 2. 방 삭제 (Cascade: 태그, 메시지)
     * 3. Redis 데이터 정리
     * 
     * @param roomId 방 ID
     * @param principal 인증된 사용자 정보
     * @throws com.vibechat.exception.UnauthorizedException 권한 없음
     */
    void deleteRoom(Long roomId, UserPrincipal principal);

    /**
     * 방 참여
     * 
     * 프로세스:
     * 1. 비공개 방: 초대 코드 검증
     * 2. 닉네임 중복 확인
     * 3. Redis에 참가자 정보 저장
     * 
     * @param roomId 방 ID
     * @param inviteCode 초대 코드 (비공개 방)
     * @param userId 사용자 ID
     * @param nickname 닉네임
     * @throws com.vibechat.exception.InvalidInviteCodeException 잘못된 초대 코드
     * @throws com.vibechat.exception.NicknameConflictException 닉네임 중복
     */
    void joinRoom(Long roomId, String inviteCode, Long userId, String nickname);

    /**
     * 방 나가기
     * 
     * @param roomId 방 ID
     * @param userId 사용자 ID
     */
    void leaveRoom(Long roomId, Long userId);
}

