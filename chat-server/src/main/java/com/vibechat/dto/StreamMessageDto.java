package com.vibechat.dto;

import com.vibechat.domain.Message;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Redis Stream을 통해 전달될 메시지의 표준 구조를 정의하는 DTO.
 * 이 DTO는 메시지 처리 파이프라인의 시작점에서 사용되는 "요청" 또는 "이벤트"의 성격을 가집니다.
 * 최종적으로 클라이언트에게 전달되는 WebSocketMessageResponse와는 역할이 분리됩니다.
 */
@Getter
@Setter
@Builder
public class StreamMessageDto {

    private String clientTempId;

    private Long roomId;

    private Long userId;

    private String nickname;

    private String avatarUrl;

    private Message.MessageType type;

    private String contentText;

    private String mediaUrl;
}
