package com.vibechat.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class RoomResponse {

    private Long id;
    private String title;
    private String description;
    private boolean isPrivate;
    private List<String> tags;
    private long participantsCount;
    private LocalDateTime lastMessageAt;
    private String inviteCode;
}
