package com.vibechat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.vibechat.domain.ChatRoom;
import com.vibechat.domain.Message;
import com.vibechat.domain.User;
import com.vibechat.dto.SendMessagePayload;
import com.vibechat.dto.UserSummaryDto;
import com.vibechat.dto.WebSocketMessageResponse;
import com.vibechat.repository.ChatRoomRepository;
import com.vibechat.repository.MessageRepository;
import com.vibechat.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class MessageServiceTest {

//    @Test
//    void 메시지_조회_limit_상한_50_적용() {
//        // given
//        MessageRepository messageRepository = mock(MessageRepository.class);
//        UserRepository userRepository = mock(UserRepository.class);
//        ChatRoomRepository chatRoomRepository = mock(ChatRoomRepository.class);
//        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
//        ModelMapper mapper = new ModelMapper();
//        MessageService svc = new MessageService(messageRepository, userRepository, chatRoomRepository, template, mapper);
//
//        Long roomId = 1L;
//        List<Message> fake = IntStream.range(0, 50).mapToObj(i -> {
//            Message m = new Message();
//            m.setId((long) i + 1);
//            m.setCreatedAt(LocalDateTime.now());
//            User u = new User(); u.setId(1L); m.setUser(u);
//            ChatRoom r = new ChatRoom(); r.setId(roomId); m.setChatRoom(r);
//            return m;
//        }).toList();
//        when(messageRepository.findByChatRoomIdOrderByIdDesc(eq(roomId), any(PageRequest.class)))
//                .thenReturn(fake);
//
//        // when
//        List<WebSocketMessageResponse> res = svc.getMessagesForRoom(roomId, null, 500);
//
//        // then
//        assertThat(res).hasSize(50);
//    }
//
//    @Test
//    void 텍스트_메시지_정상_저장_브로드캐스트() {
//        // given
//        MessageRepository messageRepository = mock(MessageRepository.class);
//        UserRepository userRepository = mock(UserRepository.class);
//        ChatRoomRepository chatRoomRepository = mock(ChatRoomRepository.class);
//        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
//        ModelMapper mapper = new ModelMapper();
//        MessageService svc = new MessageService(messageRepository, userRepository, chatRoomRepository, template, mapper);
//
//        Long roomId = 1L; Long userId = 2L;
//        User user = new User(); user.setId(userId);
//        ChatRoom room = new ChatRoom(); room.setId(roomId);
//        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
//        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(room));
//        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
//            Message m = inv.getArgument(0);
//            m.setId(100L);
//            return m;
//        });
//
//        SendMessagePayload payload = new SendMessagePayload();
//        payload.setClientTempId("tmp-1");
//        payload.setType(Message.MessageType.TEXT);
//        payload.setContentText("hello <b>world</b>");
//
//        // when
//        svc.saveAndBroadcastMessage(roomId, userId, payload);
//
//        // then
//        verify(messageRepository, times(1)).save(any(Message.class));
//        verify(template, times(1)).convertAndSend(eq("/topic/rooms/" + roomId + "/messages"), any(WebSocketMessageResponse.class));
//    }
}


