package com.vibechat.dto;

import com.vibechat.domain.Message;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendMessagePayload {

    @NotBlank
    private String clientTempId;

    @NotNull
    private Message.MessageType type;

    @Size(max = 2000)
    private String contentText;

    private String mediaUrl;
    private String mediaThumbUrl;
    private Short durationSec;
}
