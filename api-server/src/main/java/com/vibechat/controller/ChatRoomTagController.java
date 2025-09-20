package com.vibechat.controller;

import com.vibechat.dto.TagResponse;
import com.vibechat.service.tag.chatRoomTagService.ChatRoomTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class ChatRoomTagController {

    private final ChatRoomTagService tagService;

    @GetMapping("/autocomplete")
    public ResponseEntity<List<TagResponse>> autocompleteTags(@RequestParam String q) {
        List<TagResponse> tags = tagService.autocomplete(q);
        return ResponseEntity.ok(tags);
    }
}
