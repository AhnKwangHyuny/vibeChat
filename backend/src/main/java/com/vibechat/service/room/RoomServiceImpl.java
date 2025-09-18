package com.vibechat.service.room;

import com.vibechat.domain.ChatRoom;
import com.vibechat.domain.User;
import com.vibechat.dto.room.RoomCreateRequest;
import com.vibechat.dto.room.RoomResponse;
import com.vibechat.exception.NicknameConflictException;
import com.vibechat.repository.MessageRepository;
import com.vibechat.repository.UserRepository;
import com.vibechat.repository.chatRoom.ChatRoomRepository;
import com.vibechat.service.auth.AuthService;
import com.vibechat.service.tag.chatRoomTagService.ChatRoomTagService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomServiceImpl implements RoomService {

    private final ChatRoomRepository roomRepository;
    private final MessageRepository messageRepository;
    private final ChatRoomTagService chatRoomTagService;
    private final AuthService authService;
    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String ROOM_NICKNAME_KEY_PREFIX = "nickname:room:";

    @Override
    @Transactional
    public RoomResponse createRoom(@Valid @RequestBody RoomCreateRequest roomCreateRequest, HttpServletRequest request){

        authService.validateSession(request);
        String userIdString = authService.getCurrentUserId(request);
        if (userIdString == null) {
            throw new IllegalArgumentException("세션에서 유저 아이디를 찾지 못했습니다.");
        }
        Long userId = Long.parseLong(userIdString);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        // DTO -> 엔티티 변환 (ModelMapper는 더 이상 필요 없음)
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setTitle(roomCreateRequest.getTitle());
        chatRoom.setDescription(roomCreateRequest.getDescription());
        chatRoom.setPrivate(roomCreateRequest.getIsPrivate());
        chatRoom.setCreatedBy(user);

        if (roomCreateRequest.getIsPrivate()) {
            chatRoom.setInviteCode(UUID.randomUUID().toString());
        }

        // 채팅방을 먼저 저장하여 ID를 확정
        ChatRoom savedRoom = roomRepository.save(chatRoom);
        log.info("새로운 채팅방이 생성되었습니다. ID: {}", savedRoom.getId());

        // 태그 할당은 ChatRoomTagService에 위임
        if (roomCreateRequest.getTags() != null && !roomCreateRequest.getTags().isEmpty()) {
            chatRoomTagService.assignTagsToRoom(savedRoom.getId(), roomCreateRequest.getTags());
        }

        // 최종 응답 DTO 생성
        // RoomResponse.from()을 사용하지만, 동적 필드는 여기서 채워야 함
        return getRoomById(savedRoom.getId());
    }

    @Override
    public List<RoomResponse> searchRooms(List<String> tags) {
        List<ChatRoom> rooms = roomRepository.findByRoomTags_Tag_NameIn(tags);
        return rooms.stream()
                .map(this::buildRoomResponse)
                .collect(Collectors.toList());
    }

    @Override
    public RoomResponse getRoomById(Long roomId) {
        ChatRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다. ID: " + roomId));
        return buildRoomResponse(room);
    }

    @Override
    @Transactional
    public void joinRoom(Long roomId, String inviteCode, Long userId, String nickname) {
        ChatRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다. ID: " + roomId));

        if (room.isPrivate()) {
            if (inviteCode == null || !inviteCode.equals(room.getInviteCode())) {
                throw new IllegalArgumentException("유효하지 않은 초대 코드입니다.");
            }
        }

        String roomNicknameKey = ROOM_NICKNAME_KEY_PREFIX + roomId;
        if (Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(roomNicknameKey, nickname))) {
            String suggested = suggestNickname(roomNicknameKey, nickname);
            throw new NicknameConflictException("닉네임 '" + nickname + "'은 이 방에서 이미 사용 중입니다.", suggested);
        }

        redisTemplate.opsForSet().add(roomNicknameKey, nickname);
    }

    private String suggestNickname(String roomNicknameKey, String base) {
        for (int i = 0; i < 20; i++) {
            String candidate = base + String.format("%03d", (int) (Math.random() * 1000));
            if (!Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(roomNicknameKey, candidate))) {
                return candidate;
            }
        }
        return base + "_user";
    }

    /**
     * RoomResponse DTO를 빌드하는 private 헬퍼 메서드.
     * 정적 팩토리 메서드와 동적 데이터 설정을 결합합니다.
     */
    private RoomResponse buildRoomResponse(ChatRoom room) {
        // 1. 정적 팩토리 메서드로 기본 DTO 생성
        RoomResponse response = RoomResponse.from(room);

        // 2. 동적인 데이터(참여자 수, 마지막 메시지)를 서비스 계층에서 설정
        String roomSessionsKey = "presence:room:" + room.getId() + ":sessions";
        Long onlineCount = redisTemplate.opsForSet().size(roomSessionsKey);
        response.setParticipantsCount(onlineCount != null ? onlineCount : 0);

        messageRepository.findTopByChatRoomOrderByIdDesc(room)
                .ifPresent(lastMessage -> response.setLastMessageAt(lastMessage.getCreatedAt()));

        return response;
    }
}