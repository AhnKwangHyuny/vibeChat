package com.vibechat.dto;

import com.vibechat.domain.Message;
import com.vibechat.dto.UserSummaryDto;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class WebSocketMessageResponse {

    private Long id;
    private String clientTempId;
    private Long roomId;
    private UserSummaryDto user;
    private Message.MessageType type;
    private String contentText;
    private String mediaUrl;
    private String mediaThumbUrl;
    private Short mediaDurationSec;
    private LocalDateTime createdAt;
}
