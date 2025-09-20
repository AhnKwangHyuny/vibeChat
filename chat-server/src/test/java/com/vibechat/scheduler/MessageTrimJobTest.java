package com.vibechat.scheduler;

import static org.mockito.Mockito.*;

import com.vibechat.repository.chatRoom.ChatRoomRepository;
import com.vibechat.repository.MessageRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class MessageTrimJobTest {

    @Test
    void 모든_방에_대해_최신_1000개만_유지() {
        // given
        MessageRepository messageRepository = mock(MessageRepository.class);
        ChatRoomRepository chatRoomRepository = mock(ChatRoomRepository.class);
        when(chatRoomRepository.findAllIds()).thenReturn(List.of(1L, 2L, 3L));
        MessageTrimJob job = new MessageTrimJob(messageRepository, chatRoomRepository);

        // when
        job.trimOldMessages();

        // then
        verify(messageRepository, times(1)).trimOldMessages(1L, 1000);
        verify(messageRepository, times(1)).trimOldMessages(2L, 1000);
        verify(messageRepository, times(1)).trimOldMessages(3L, 1000);
    }
}


