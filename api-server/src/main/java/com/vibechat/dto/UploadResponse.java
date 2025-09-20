package com.vibechat.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vibechat.domain.Message;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UploadResponse {
    private Message.MessageType type;
    private String url;
    private String thumbUrl;
    private Short durationSec;
}
