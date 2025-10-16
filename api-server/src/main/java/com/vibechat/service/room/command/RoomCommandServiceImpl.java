package com.vibechat.service.room.command;

import com.vibechat.domain.ChatRoom;
import com.vibechat.domain.User;
import com.vibechat.domain.auth.UserPrincipal;
import com.vibechat.dto.room.RoomCreateRequest;
import com.vibechat.dto.room.RoomResponse;
import com.vibechat.exception.NicknameConflictException;
import com.vibechat.exception.room.InvalidInviteCodeException;
import com.vibechat.exception.room.RoomAccessDeniedException;
import com.vibechat.exception.room.RoomCreationException;
import com.vibechat.exception.room.RoomNotFoundException;
import com.vibechat.repository.UserRepository;
import com.vibechat.repository.chatRoom.ChatRoomRepository;
import com.vibechat.service.room.query.RoomQueryService;
import com.vibechat.service.tag.chatRoomTagService.ChatRoomTagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 방 생성/수정/삭제 서비스 구현체
 * 
 * SRP: 방 변경 작업만 담당
 * - 조회는 RoomQueryService
 * - 목록 조회는 RoomListService
 * 
 * 트랜잭션:
 * - 방 생성/수정/삭제는 쓰기 트랜잭션
 * - 조회는 별도 서비스 (읽기 전용)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoomCommandServiceImpl implements RoomCommandService {

    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final ChatRoomTagService chatRoomTagService;
    private final RoomQueryService roomQueryService; // 조회는 별도 서비스
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String ROOM_NICKNAME_KEY_PREFIX = "room:nickname:";

    @Override
    @Transactional
    public RoomResponse createRoom(RoomCreateRequest request, UserPrincipal principal) {
        log.info("[RoomCommand] 방 생성 시작: title={}, userId={}", 
            request.getTitle(), principal.id());

        try {
            // 1. 사용자 조회
            User user = userRepository.findById(principal.id())
                .orElseThrow(() -> {
                    log.error("[RoomCommand] 사용자를 찾을 수 없음: userId={}", principal.id());
                    return new RoomCreationException("사용자를 찾을 수 없습니다.");
                });

            // 2. 방 엔티티 생성
            ChatRoom chatRoom = new ChatRoom();
            chatRoom.setTitle(request.getTitle());
            chatRoom.setDescription(request.getDescription());
            chatRoom.setPrivate(request.getIsPrivate());
            chatRoom.setCreatedBy(user);

            // 3. 비공개 방: 초대 코드 생성
            if (request.getIsPrivate()) {
                String inviteCode = generateInviteCode();
                chatRoom.setInviteCode(inviteCode);
                log.debug("[RoomCommand] 초대 코드 생성: {}", inviteCode);
            }

            // 4. 방 저장
            ChatRoom savedRoom = chatRoomRepository.save(chatRoom);
            log.info("[RoomCommand] 방 생성 완료: roomId={}, title={}", 
                savedRoom.getId(), savedRoom.getTitle());

            // 5. 태그 할당
            if (request.getTags() != null && !request.getTags().isEmpty()) {
                chatRoomTagService.assignTagsToRoom(savedRoom.getId(), request.getTags());
                log.debug("[RoomCommand] 태그 할당 완료: {} 개", request.getTags().size());
            }

            // 6. 생성된 방 조회 (RoomQueryService 사용)
            return roomQueryService.getRoomById(savedRoom.getId());

        } catch (RoomCreationException e) {
            throw e; // 이미 적절한 예외이므로 그대로 던짐
        } catch (Exception e) {
            log.error("[RoomCommand] 방 생성 실패: title={}", request.getTitle(), e);
            throw new RoomCreationException("방 생성 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    @Transactional
    public RoomResponse updateRoom(Long roomId, RoomCreateRequest request, UserPrincipal principal) {
        log.info("[RoomCommand] 방 수정 시작: roomId={}, userId={}", roomId, principal.id());

        // 1. 방 조회
        ChatRoom room = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> {
                log.warn("[RoomCommand] 방을 찾을 수 없음: roomId={}", roomId);
                return new RoomNotFoundException(roomId);
            });

        // 2. 권한 확인 (방장만 수정 가능)
        if (!room.getCreatedBy().getId().equals(principal.id())) {
            log.warn("[RoomCommand] 수정 권한 없음: roomId={}, userId={}, creatorId={}", 
                roomId, principal.id(), room.getCreatedBy().getId());
            throw new RoomAccessDeniedException(roomId, principal.id(), "수정");
        }

        // 3. 방 정보 수정
        room.setTitle(request.getTitle());
        room.setDescription(request.getDescription());
        room.setPrivate(request.getIsPrivate());

        // 4. 공개/비공개 전환 처리
        if (request.getIsPrivate() && room.getInviteCode() == null) {
            room.setInviteCode(generateInviteCode());
        } else if (!request.getIsPrivate()) {
            room.setInviteCode(null); // 공개 방으로 전환 시 초대 코드 제거
        }

        // 5. 태그 재할당
        if (request.getTags() != null) {
            chatRoomTagService.assignTagsToRoom(roomId, request.getTags());
        }

        chatRoomRepository.save(room);
        log.info("[RoomCommand] 방 수정 완료: roomId={}", roomId);

        return roomQueryService.getRoomById(roomId);
    }

    @Override
    @Transactional
    public void deleteRoom(Long roomId, UserPrincipal principal) {
        log.info("[RoomCommand] 방 삭제 시작: roomId={}, userId={}", roomId, principal.id());

        // 1. 방 조회
        ChatRoom room = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> {
                log.warn("[RoomCommand] 방을 찾을 수 없음: roomId={}", roomId);
                return new RoomNotFoundException(roomId);
            });

        // 2. 권한 확인 (방장만 삭제 가능)
        if (!room.getCreatedBy().getId().equals(principal.id())) {
            log.warn("[RoomCommand] 삭제 권한 없음: roomId={}, userId={}, creatorId={}", 
                roomId, principal.id(), room.getCreatedBy().getId());
            throw new RoomAccessDeniedException(roomId, principal.id(), "삭제");
        }

        // 3. 방 삭제 (Cascade: 태그, 메시지 자동 삭제)
        chatRoomRepository.delete(room);

        // 4. Redis 데이터 정리
        String nicknameKey = ROOM_NICKNAME_KEY_PREFIX + roomId;
        redisTemplate.delete(nicknameKey);

        log.info("[RoomCommand] 방 삭제 완료: roomId={}", roomId);
    }

    @Override
    @Transactional
    public void joinRoom(Long roomId, String inviteCode, Long userId, String nickname) {
        log.info("[RoomCommand] 방 참여 시작: roomId={}, userId={}, nickname={}", 
            roomId, userId, nickname);

        // 1. 방 조회
        ChatRoom room = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> {
                log.warn("[RoomCommand] 방을 찾을 수 없음: roomId={}", roomId);
                return new RoomNotFoundException(roomId);
            });

        // 2. 비공개 방: 초대 코드 검증
        if (room.isPrivate()) {
            if (inviteCode == null || !inviteCode.equals(room.getInviteCode())) {
                log.warn("[RoomCommand] 잘못된 초대 코드: roomId={}, providedCode={}", 
                    roomId, inviteCode);
                throw new InvalidInviteCodeException(roomId, inviteCode);
            }
        }

        // 3. 닉네임 중복 확인
        String roomNicknameKey = ROOM_NICKNAME_KEY_PREFIX + roomId;
        if (Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(roomNicknameKey, nickname))) {
            String suggested = suggestNickname(roomNicknameKey, nickname);
            log.warn("[RoomCommand] 닉네임 중복: roomId={}, nickname={}, suggested={}", 
                roomId, nickname, suggested);
            throw new NicknameConflictException(
                "닉네임 '" + nickname + "'은 이 방에서 이미 사용 중입니다.", 
                suggested
            );
        }

        // 4. Redis에 닉네임 저장
        redisTemplate.opsForSet().add(roomNicknameKey, nickname);
        
        log.info("[RoomCommand] 방 참여 완료: roomId={}, userId={}, nickname={}", 
            roomId, userId, nickname);
    }

    @Override
    @Transactional
    public void leaveRoom(Long roomId, Long userId) {
        log.info("[RoomCommand] 방 나가기: roomId={}, userId={}", roomId, userId);

        // TODO: Redis에서 닉네임 제거 로직 구현
        // - userId와 nickname 매핑 필요
        // - 현재는 nickname을 모르는 상태
        
        log.info("[RoomCommand] 방 나가기 완료: roomId={}, userId={}", roomId, userId);
    }

    /**
     * 초대 코드 생성
     */
    private String generateInviteCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * 닉네임 중복 시 대안 제안
     */
    private String suggestNickname(String roomNicknameKey, String base) {
        for (int i = 0; i < 20; i++) {
            String candidate = base + String.format("%03d", (int) (Math.random() * 1000));
            if (!Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(roomNicknameKey, candidate))) {
                return candidate;
            }
        }
        return base + "_user";
    }
}

