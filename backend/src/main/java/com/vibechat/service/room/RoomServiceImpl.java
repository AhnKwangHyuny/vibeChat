package com.vibechat.service.room;

import com.vibechat.domain.ChatRoom;
import com.vibechat.domain.RoomTag;
import com.vibechat.domain.Tag;
import com.vibechat.domain.User;
import com.vibechat.dto.RoomCreateRequest;
import com.vibechat.dto.RoomResponse;
import com.vibechat.exception.NicknameConflictException;
import com.vibechat.repository.ChatRoomRepository;
import com.vibechat.repository.MessageRepository;
import com.vibechat.repository.RoomTagRepository;
import com.vibechat.repository.UserRepository;
import com.vibechat.service.tag.TagServiceImpl;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomServiceImpl implements RoomService {

    private final ChatRoomRepository roomRepository;
    private final MessageRepository messageRepository;
    private final TagServiceImpl tagService;
    private final RoomTagRepository roomTagRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String ROOM_NICKNAME_KEY_PREFIX = "nickname:room:";

    @Override
    @Transactional
    public RoomResponse createRoom(RoomCreateRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        ChatRoom chatRoom = modelMapper.map(request, ChatRoom.class);
        chatRoom.setCreatedBy(user);

        if (request.getIsPrivate()) {
            chatRoom.setInviteCode(UUID.randomUUID().toString());
        }

        ChatRoom savedRoom = roomRepository.save(chatRoom);

        List<Tag> tags = tagService.findOrCreateTags(request.getTags());
        tags.forEach(tag -> {
            RoomTag roomTag = new RoomTag();
            roomTag.setChatRoom(savedRoom);
            roomTag.setTag(tag);
            roomTagRepository.save(roomTag);
        });

        RoomResponse response = modelMapper.map(savedRoom, RoomResponse.class);
        response.setTags(tags.stream().map(Tag::getName).collect(Collectors.toList()));
        return response;
    }

    @Override
    public List<RoomResponse> searchRooms(List<String> tags) {
        List<ChatRoom> rooms = roomRepository.findByRoomTags_Tag_NameIn(tags);
        return rooms.stream()
                .map(this::mapChatRoomToRoomResponse)
                .collect(Collectors.toList());
    }

    @Override
    public RoomResponse getRoomById(Long roomId) {
        ChatRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));
        return mapChatRoomToRoomResponse(room);
    }

    @Override
    @Transactional
    public void joinRoom(Long roomId, String inviteCode, Long userId, String nickname) {
        ChatRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        if (room.isPrivate()) {
            if (inviteCode == null || !inviteCode.equals(room.getInviteCode())) {
                throw new IllegalArgumentException("Invalid invite code");
            }
        }

        String roomNicknameKey = ROOM_NICKNAME_KEY_PREFIX + roomId;
        if (Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(roomNicknameKey, nickname))) {
            String suggested = suggestNickname(roomNicknameKey, nickname);
            throw new NicknameConflictException("Nickname '" + nickname + "' is already taken in this room.", suggested);
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

    private RoomResponse mapChatRoomToRoomResponse(ChatRoom room) {
        RoomResponse response = modelMapper.map(room, RoomResponse.class);
        response.setTags(room.getRoomTags().stream().map(rt -> rt.getTag().getName()).collect(Collectors.toList()));
        String roomSessionsKey = "presence:room:" + room.getId() + ":sessions";
        Long onlineCount = redisTemplate.opsForSet().size(roomSessionsKey);
        response.setParticipantsCount(onlineCount != null ? onlineCount : 0);
        messageRepository.findTopByChatRoomOrderByIdDesc(room)
                .ifPresent(lastMessage -> response.setLastMessageAt(lastMessage.getCreatedAt()));
        return response;
    }
}


