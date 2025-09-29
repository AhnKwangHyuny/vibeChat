package com.vibechat.dto;

import com.vibechat.domain.MessageType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class WebSocketMessageResponse {

    private Long id;
    private String clientTempId;
    private Long roomId;
    private UserSummaryDto user;
    private MessageType type;
    private String contentText;
    private String mediaUrl;
    private String mediaThumbUrl;
    private Short mediaDurationSec;
    private LocalDateTime createdAt;
}
