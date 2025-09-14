package com.vibechat.service.room;

import com.vibechat.domain.ChatRoom;
import com.vibechat.domain.RoomTag;
import com.vibechat.domain.User;
import com.vibechat.dto.RoomCreateRequest;
import com.vibechat.dto.RoomResponse;
import com.vibechat.exception.NicknameConflictException;
import com.vibechat.repository.ChatRoomRepository;
import com.vibechat.repository.MessageRepository;
import com.vibechat.repository.UserRepository;
import com.vibechat.service.auth.AuthService;
import com.vibechat.service.tag.chatRoomTagService.ChatRoomTagService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
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
    private final ModelMapper modelMapper;
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

        ChatRoom chatRoom = modelMapper.map(roomCreateRequest, ChatRoom.class);
        chatRoom.setCreatedBy(user);

        if (roomCreateRequest.getIsPrivate()) {
            chatRoom.setInviteCode(UUID.randomUUID().toString());
        }

        // room id 생성
        ChatRoom savedRoom = roomRepository.save(chatRoom);

        // tags => room 연관관계 매핑
        List<RoomTag> tags = chatRoomTagService.createTags(roomCreateRequest.getTags() , savedRoom.getId());

        // 응답 response 생성
        RoomResponse response = modelMapper.map(savedRoom, RoomResponse.class);

        System.out.println("response = " + response);

        return response;
    }

//    @Override
//    public List<RoomResponse> searchRooms(List<String> tags) {
////        List<ChatRoom> rooms = roomRepository.findByRoomTags_Tag_NameIn(tags);
////        return rooms.stream()
////                .map(this::mapChatRoomToRoomResponse)
////                .collect(Collectors.toList());
//    }
//
//    @Override
//    public RoomResponse getRoomById(Long roomId) {
////        ChatRoom room = roomRepository.findById(roomId)
////                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));
////        return mapChatRoomToRoomResponse(room);
//    }

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

    private RoomResponse mapChatRoomToRoomResponse(ChatRoom room , List<RoomTag> tags) {
        RoomResponse response = modelMapper.map(room, RoomResponse.class);
        response.setTags(tags.stream().map(RoomTag::getName).collect(Collectors.toList()));
        String roomSessionsKey = "presence:room:" + room.getId() + ":sessions";
        Long onlineCount = redisTemplate.opsForSet().size(roomSessionsKey);
        response.setParticipantsCount(onlineCount != null ? onlineCount : 0);
        messageRepository.findTopByChatRoomOrderByIdDesc(room)
                .ifPresent(lastMessage -> response.setLastMessageAt(lastMessage.getCreatedAt()));
        return response;
    }
}


