package com.vibechat.service.room.query;

import com.vibechat.domain.ChatRoom;
import com.vibechat.dto.room.RoomResponse;
import com.vibechat.exception.room.RoomNotFoundException;
import com.vibechat.repository.MessageRepository;
import com.vibechat.repository.chatRoom.ChatRoomRepository;
import com.vibechat.service.room.participants.RoomParticipantCountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 방 조회 서비스 구현체
 * 
 * SRP: 단일 방 조회만 담당
 * - 방 생성/수정/삭제는 RoomCommandService
 * - 방 목록 조회는 RoomListService
 * 
 * 최적화:
 * - 읽기 전용 트랜잭션
 * - 참가자 수는 Redis에서 조회
 * - 마지막 메시지는 선택적 로드
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RoomQueryServiceImpl implements RoomQueryService {

    private final ChatRoomRepository chatRoomRepository;
    private final RoomParticipantCountService participantCountService;
    private final MessageRepository messageRepository;

    @Override
    public RoomResponse getRoomById(Long roomId) {
        log.debug("[RoomQuery] 방 조회: roomId={}", roomId);

        ChatRoom room = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> {
                log.warn("[RoomQuery] 방을 찾을 수 없음: roomId={}", roomId);
                return new RoomNotFoundException(roomId);
            });

        return buildRoomResponse(room);
    }

    @Override
    public Optional<RoomResponse> findRoomById(Long roomId) {
        log.debug("[RoomQuery] 방 조회 (Optional): roomId={}", roomId);

        return chatRoomRepository.findById(roomId)
            .map(this::buildRoomResponse);
    }

    @Override
    public boolean existsById(Long roomId) {
        boolean exists = chatRoomRepository.existsById(roomId);
        
        if (log.isDebugEnabled()) {
            log.debug("[RoomQuery] 방 존재 여부: roomId={}, exists={}", roomId, exists);
        }
        
        return exists;
    }

    @Override
    public RoomResponse getRoomByInviteCode(String inviteCode) {
        log.debug("[RoomQuery] 초대 코드로 조회: inviteCode={}", inviteCode);

        // TODO: Repository에 findByInviteCode 메서드 추가 필요
        throw new UnsupportedOperationException("초대 코드 조회 기능은 구현 예정입니다.");
    }

    /**
     * ChatRoom → RoomResponse 변환
     * 
     * 포함 정보:
     * - 기본 방 정보 (from 팩토리 메서드)
     * - 실시간 참가자 수 (Redis)
     * - 마지막 메시지 시각 (DB)
     */
    private RoomResponse buildRoomResponse(ChatRoom room) {
        // 1. 기본 정보 변환
        RoomResponse response = RoomResponse.from(room);

        // 2. 실시간 참가자 수 조회 (Redis)
        int participantCount = participantCountService.getParticipantCount(room.getId());
        response.setParticipantsCount(participantCount);

        // 3. 마지막 메시지 시각 조회 (선택적)
        messageRepository.findTopByChatRoomOrderByIdDesc(room)
            .ifPresent(lastMessage -> {
                response.setLastMessageAt(lastMessage.getCreatedAt());
                
                if (log.isDebugEnabled()) {
                    log.debug("[RoomQuery] 마지막 메시지: roomId={}, messageAt={}", 
                        room.getId(), lastMessage.getCreatedAt());
                }
            });

        if (log.isDebugEnabled()) {
            log.debug("[RoomQuery] 방 응답 생성 완료: roomId={}, participants={}", 
                room.getId(), participantCount);
        }

        return response;
    }
}

