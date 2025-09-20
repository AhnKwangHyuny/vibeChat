package com.vibechat.service.message;

import com.vibechat.domain.ChatRoom;
import com.vibechat.domain.User;
import com.vibechat.dto.SendMessagePayload;
import com.vibechat.domain.Message;
import java.util.List;

public interface MessageService {
    void saveAndBroadcastMessage(Long roomId, Long userId, SendMessagePayload payload);
    List<Message> getMessagesForRoom(Long roomId, Long beforeId, int limit);
    Message saveMessage(ChatRoom room, User user, SendMessagePayload payload);
}


