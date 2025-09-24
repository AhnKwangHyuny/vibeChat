package com.vibechat.service.message;

import com.vibechat.dto.SendMessagePayload;

/**
 * Chat-Server의 메시지 서비스
 * - 실시간 브로드캐스트만 담당
 * - DB 저장은 API-Server의 책임
 */
public interface MessageService {
    void broadcastMessage(Long roomId, String nickname, String avatarUrl, SendMessagePayload payload);
}


