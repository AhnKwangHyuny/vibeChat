package com.vibechat.service.room;

import com.vibechat.domain.ChatRoom;
import com.vibechat.domain.User;
import com.vibechat.domain.auth.UserPrincipal;
import com.vibechat.dto.room.RoomCreateRequest;
import com.vibechat.dto.room.RoomResponse;
import com.vibechat.exception.NicknameConflictException;
import com.vibechat.repository.MessageRepository;
import com.vibechat.repository.UserRepository;
import com.vibechat.repository.chatRoom.ChatRoomRepository;
import com.vibechat.service.tag.chatRoomTagService.ChatRoomTagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String ROOM_NICKNAME_KEY_PREFIX = "nickname:room:";

    @Override
    @Transactional
    public RoomResponse createRoom(RoomCreateRequest roomCreateRequest, UserPrincipal principal) {

        if (principal == null) {
            throw new IllegalArgumentException("인증 정보가 없습니다.");
        }

        if (principal.id() == null) {
            throw new IllegalArgumentException("사용자 ID가 없습니다. UserPrincipal: " + principal);
        }

        User user = userRepository.findById(principal.id())
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다. ID: " + principal.id()));

        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setTitle(roomCreateRequest.getTitle());
        chatRoom.setDescription(roomCreateRequest.getDescription());
        chatRoom.setPrivate(roomCreateRequest.getIsPrivate());
        chatRoom.setCreatedBy(user);

        if (roomCreateRequest.getIsPrivate()) {
            chatRoom.setInviteCode(UUID.randomUUID().toString());
        }

        ChatRoom savedRoom = roomRepository.save(chatRoom);
        log.info("새로운 채팅방이 생성되었습니다. ID: {}", savedRoom.getId());

        if (roomCreateRequest.getTags() != null && !roomCreateRequest.getTags().isEmpty()) {
            chatRoomTagService.assignTagsToRoom(savedRoom.getId(), roomCreateRequest.getTags());
        }

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

    private RoomResponse buildRoomResponse(ChatRoom room) {
        RoomResponse response = RoomResponse.from(room);

        String roomSessionsKey = "presence:room:" + room.getId() + ":sessions";
        Long onlineCount = redisTemplate.opsForSet().size(roomSessionsKey);
        response.setParticipantsCount(onlineCount != null ? onlineCount : 0);

        messageRepository.findTopByChatRoomOrderByIdDesc(room)
                .ifPresent(lastMessage -> response.setLastMessageAt(lastMessage.getCreatedAt()));

        return response;
    }
}
