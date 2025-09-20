package com.vibechat.service.message;

import com.vibechat.domain.ChatRoom;
import com.vibechat.domain.Message;
import com.vibechat.domain.User;
import com.vibechat.dto.SendMessagePayload;
import com.vibechat.dto.UserSummaryDto;
import com.vibechat.repository.chatRoom.ChatRoomRepository;
import com.vibechat.repository.MessageRepository;
import com.vibechat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ModelMapper modelMapper;

    private static final PolicyFactory SANITIZER_POLICY = Sanitizers.FORMATTING.and(Sanitizers.LINKS);

    @Override
    @Transactional
    public void saveAndBroadcastMessage(Long roomId, Long userId, SendMessagePayload payload) {
        // Temporarily empty implementation
    }

    @Override
    public List<Message> getMessagesForRoom(Long roomId, Long beforeId, int limit) {
        try {
            int capped = Math.min(limit, 50);
            Pageable pageable = PageRequest.of(0, capped);
            List<Message> messages = (beforeId == null)
                    ? messageRepository.findByChatRoomIdOrderByIdDesc(roomId, pageable)
                    : messageRepository.findByChatRoomIdAndIdLessThanOrderByIdDesc(roomId, beforeId, pageable);

            return messages;
        } catch (Exception e) {
            log.error("Error fetching messages for room: {}", roomId, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public Message saveMessage(ChatRoom room, User user, SendMessagePayload payload) {
        try {
            if ("TEXT".equals(payload.getType())) {
                if (payload.getContentText() == null || payload.getContentText().trim().isEmpty()) {
                    throw new IllegalArgumentException("Text message cannot be empty");
                }
                if (payload.getContentText().length() > 2000) {
                    throw new IllegalArgumentException("Message too long (max 2000 characters)");
                }
            }

            Message message = new Message();
            message.setChatRoom(room);
            message.setUser(user);
            message.setClientTempId(payload.getClientTempId());
            message.setType(payload.getType());
            if (payload.getContentText() != null) {
                String sanitizedText = SANITIZER_POLICY.sanitize(payload.getContentText());
                message.setContentText(sanitizedText);
            }
            message.setMediaUrl(payload.getMediaUrl());
            message.setMediaThumbUrl(payload.getMediaThumbUrl());
            message.setMediaDurationSec(payload.getDurationSec());
            message.setCreatedAt(LocalDateTime.now());
            return messageRepository.save(message);
        } catch (Exception e) {
            throw e;
        }
    }
}


