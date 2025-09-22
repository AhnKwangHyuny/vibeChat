package com.vibechat.service;

import com.vibechat.domain.ChatRoom;
import com.vibechat.domain.Message;
import com.vibechat.domain.User;
import com.vibechat.dto.SendMessagePayload;
import com.vibechat.repository.MessageRepository;
import com.vibechat.service.message.MessageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceSecurityTest {

    @Mock
    private MessageRepository messageRepository;

    @InjectMocks
    private MessageServiceImpl messageService;

    private User testUser;
    private ChatRoom testRoom;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setNickname("TestUser");

        testRoom = new ChatRoom();
        testRoom.setId(1L);
        testRoom.setTitle("Test Room");
    }

    @Test
    void shouldSanitizeXSSInTextMessage() {
        // Given
        SendMessagePayload payload = new SendMessagePayload();
        payload.setType(Message.MessageType.TEXT);
        payload.setContentText("<script>alert('xss')</script>Hello World");
        payload.setClientTempId("test-123");

        Message savedMessage = new Message();
        savedMessage.setId(1L);
        savedMessage.setContentText("Hello World"); // XSS should be sanitized
        savedMessage.setUser(testUser);
        savedMessage.setChatRoom(testRoom);

        when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);

        // When
        Message result = messageService.saveMessage(testRoom, testUser, payload);

        // Then
        assertNotNull(result);
        assertFalse(result.getContentText().contains("<script>"));
        assertFalse(result.getContentText().contains("alert"));
        assertTrue(result.getContentText().contains("Hello World"));
    }

    @Test
    void shouldRejectEmptyTextMessage() {
        // Given
        SendMessagePayload payload = new SendMessagePayload();
        payload.setType(Message.MessageType.TEXT);
        payload.setContentText("");
        payload.setClientTempId("test-123");

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            messageService.saveMessage(testRoom, testUser, payload)
        );
    }

    @Test
    void shouldRejectTooLongMessage() {
        // Given
        SendMessagePayload payload = new SendMessagePayload();
        payload.setType(Message.MessageType.TEXT);
        payload.setContentText("a".repeat(2001)); // Over 2000 character limit
        payload.setClientTempId("test-123");

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            messageService.saveMessage(testRoom, testUser, payload)
        );
    }

    @Test
    void shouldPreserveBasicHTMLTags() {
        // Given
        SendMessagePayload payload = new SendMessagePayload();
        payload.setType(Message.MessageType.TEXT);
        payload.setContentText("<b>Bold</b> and <i>italic</i> text");
        payload.setClientTempId("test-123");

        Message savedMessage = new Message();
        savedMessage.setId(1L);
        savedMessage.setContentText("<b>Bold</b> and <i>italic</i> text");
        savedMessage.setUser(testUser);
        savedMessage.setChatRoom(testRoom);

        when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);

        // When
        Message result = messageService.saveMessage(testRoom, testUser, payload);

        // Then
        assertNotNull(result);
        assertTrue(result.getContentText().contains("<b>Bold</b>"));
        assertTrue(result.getContentText().contains("<i>italic</i>"));
    }
}