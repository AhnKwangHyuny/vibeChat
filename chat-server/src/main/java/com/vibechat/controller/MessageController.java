package com.vibechat.controller;

import com.vibechat.dto.WebSocketMessageResponse;
import com.vibechat.service.message.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms/{roomId}/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping
    public ResponseEntity<List<WebSocketMessageResponse>> getMessages(
            @PathVariable Long roomId,
            @RequestParam(required = false) Long beforeId,
            @RequestParam(defaultValue = "30") int limit) {
        
        // max limit checking
        if (limit > 50) {
            limit = 50;
        }

        List<WebSocketMessageResponse> messages = messageService.getMessagesForRoom(roomId, beforeId, limit);
        return ResponseEntity.ok(messages);
    }
}
