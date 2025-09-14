package com.vibechat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.vibechat.domain.ChatRoom;
import com.vibechat.exception.NicknameConflictException;
import com.vibechat.repository.ChatRoomRepository;
import com.vibechat.repository.MessageRepository;
import com.vibechat.repository.RoomTagRepository;
import com.vibechat.repository.UserRepository;

import java.util.Optional;

import com.vibechat.service.room.RoomService;
import com.vibechat.service.tag.TagServiceImpl;
import com.vibechat.service.tag.chatRoomTagService.ChatRoomTagServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock ChatRoomRepository roomRepository;
    @Mock MessageRepository messageRepository;
    @Mock ChatRoomTagServiceImpl tagService;
    @Mock RoomTagRepository roomTagRepository;
    @Mock UserRepository userRepository;
    @Mock ModelMapper modelMapper;
    @Mock RedisTemplate<String, String> redisTemplate;
    @Mock SetOperations<String, String> setOps;

    @InjectMocks RoomService roomService;

    @BeforeEach
    void setup() {
        when(redisTemplate.opsForSet()).thenReturn(setOps);
    }

    @Test
    void 닉네임_중복시_409와_제안닉네임_반환() {
        // given
        Long roomId = 1L;
        Long userId = 10L;
        String nickname = "john";
        ChatRoom room = new ChatRoom();
        room.setId(roomId);
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(setOps.isMember("nickname:room:" + roomId, nickname)).thenReturn(true);

        // when / then
        assertThatThrownBy(() -> roomService.joinRoom(roomId, null, userId, nickname))
                .isInstanceOf(NicknameConflictException.class)
                .satisfies(ex -> {
                    NicknameConflictException nce = (NicknameConflictException) ex;
                    assertThat(nce.getSuggestedNickname()).isNotBlank();
                });
    }

    @Test
    void 닉네임_정상가입시_세트추가() {
        // given
        Long roomId = 2L;
        Long userId = 10L;
        String nickname = "alice";
        ChatRoom room = new ChatRoom();
        room.setId(roomId);
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(setOps.isMember("nickname:room:" + roomId, nickname)).thenReturn(false);

        // when
        roomService.joinRoom(roomId, null, userId, nickname);

        // then
        verify(setOps, times(1)).add("nickname:room:" + roomId, nickname);
    }
}
