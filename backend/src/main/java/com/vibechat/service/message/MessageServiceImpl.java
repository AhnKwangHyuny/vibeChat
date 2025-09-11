package com.vibechat.service.message;

import com.vibechat.domain.ChatRoom;
import com.vibechat.domain.Message;
import com.vibechat.domain.User;
import com.vibechat.dto.SendMessagePayload;
import com.vibechat.dto.UserSummaryDto;
import com.vibechat.dto.WebSocketMessageResponse;
import com.vibechat.repository.ChatRoomRepository;
import com.vibechat.repository.MessageRepository;
import com.vibechat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
    private final SimpMessagingTemplate messagingTemplate;
    private final ModelMapper modelMapper;

    private static final PolicyFactory SANITIZER_POLICY = Sanitizers.FORMATTING.and(Sanitizers.LINKS);

    @Override
    @Transactional
    public void saveAndBroadcastMessage(Long roomId, Long userId, SendMessagePayload payload) {
        try {
            log.debug("Saving message for room: {}, user: {}, type: {}", roomId, userId, payload.getType());
            User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
            ChatRoom room = chatRoomRepository.findById(roomId).orElseThrow(() -> new IllegalArgumentException("Room not found"));

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
                if (!sanitizedText.equals(payload.getContentText())) {
                    log.warn("Message content was sanitized for user: {}", userId);
                }
            }
            message.setMediaUrl(payload.getMediaUrl());
            message.setMediaThumbUrl(payload.getMediaThumbUrl());
            message.setMediaDurationSec(payload.getDurationSec());
            message.setCreatedAt(LocalDateTime.now());

            Message savedMessage = messageRepository.save(message);

            WebSocketMessageResponse response = new WebSocketMessageResponse();
            response.setId(savedMessage.getId());
            response.setClientTempId(savedMessage.getClientTempId());
            response.setRoomId(savedMessage.getChatRoom().getId());
            response.setUser(modelMapper.map(user, UserSummaryDto.class));
            response.setType(savedMessage.getType());
            response.setContentText(savedMessage.getContentText());
            response.setMediaUrl(savedMessage.getMediaUrl());
            response.setMediaThumbUrl(savedMessage.getMediaThumbUrl());
            response.setMediaDurationSec(savedMessage.getMediaDurationSec());
            response.setCreatedAt(savedMessage.getCreatedAt());

            messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/messages", response);
        } catch (Exception e) {
            log.error("Error saving/broadcasting message for room: {}, user: {}", roomId, userId, e);
            throw e;
        }
    }

    @Override
    public List<WebSocketMessageResponse> getMessagesForRoom(Long roomId, Long beforeId, int limit) {
        try {
            int capped = Math.min(limit, 50);
            Pageable pageable = PageRequest.of(0, capped);
            List<Message> messages = (beforeId == null)
                    ? messageRepository.findByChatRoomIdOrderByIdDesc(roomId, pageable)
                    : messageRepository.findByChatRoomIdAndIdLessThanOrderByIdDesc(roomId, beforeId, pageable);

            return messages.stream()
                    .map(message -> {
                        WebSocketMessageResponse response = modelMapper.map(message, WebSocketMessageResponse.class);
                        response.setUser(modelMapper.map(message.getUser(), UserSummaryDto.class));
                        response.setRoomId(message.getChatRoom().getId());
                        return response;
                    })
                    .collect(Collectors.toList());
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


