package com.vibechat.domain.message;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(collection = "chat_messages")
@CompoundIndexes({
    @CompoundIndex(name = "idx_roomIdentifier_timestamp", def = "{'roomIdentifier': 1, 'timestamp': -1}"),
    @CompoundIndex(name = "idx_userIdentifier_timestamp", def = "{'userIdentifier': 1, 'timestamp': -1}"),
    @CompoundIndex(name = "idx_roomIdentifier_type_timestamp", def = "{'roomIdentifier': 1, 'type': 1, 'timestamp': -1}"),
    @CompoundIndex(name = "idx_roomIdentifier_notDeleted_timestamp", def = "{'roomIdentifier': 1, 'status.isDeleted': 1, 'timestamp': -1}")
})
@Data
@Builder
public class ChatMessage {

    @Id
    private String id;

    private String messageId;           // 비즈니스 식별자

    @Indexed
    private String roomIdentifier;      // API-Server가 제공한 방 식별자 (샤딩 키)

    @Indexed
    private String userIdentifier;      // API-Server가 제공한 유저 식별자

    private UserInfo userInfo;          // 비정규화된 사용자 정보 (API-Server에서 제공)

    @Indexed
    private String type;                // TEXT, IMAGE, GIF, VIDEO, SYSTEM, FILE

    private MessageContent content;     // 메시지 내용

    private ClientInfo clientInfo;      // 클라이언트 정보

    private MessageStatus status;       // 메시지 상태

    private DeliveryInfo delivery;      // 전달 정보

    private MessageMetrics metrics;     // 분석용 메트릭

    @Indexed
    private LocalDateTime timestamp;    // 메시지 생성 시간

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class UserInfo {
        private String nickname;
        private String avatarUrl;
    }

    @Data
    @Builder
    public static class MessageContent {
        private String text;                    // 텍스트 메시지
        private MediaInfo media;                // 미디어 정보
        private SystemInfo system;              // 시스템 메시지
    }

    @Data
    @Builder
    public static class MediaInfo {
        private String url;
        private String thumbnailUrl;
        private String mimeType;
        private Long size;
        private Double duration;
        private Integer width;
        private Integer height;
        private Map<String, Object> metadata;   // 확장 가능한 메타데이터
    }

    @Data
    @Builder
    public static class SystemInfo {
        private String action;                  // USER_JOINED, USER_LEFT, etc.
        private Map<String, Object> data;       // 시스템 메시지 관련 데이터
    }

    @Data
    @Builder
    public static class ClientInfo {
        @Indexed(expireAfterSeconds = 86400)    // 24시간 후 자동 삭제
        private String tempId;                  // 클라이언트 임시 ID
        private String platform;                // WEB, IOS, ANDROID
        private String version;                 // 클라이언트 버전
    }

    @Data
    @Builder
    public static class MessageStatus {
        private Boolean isDeleted;
        private Boolean isEdited;
        private LocalDateTime editedAt;
        private LocalDateTime deletedAt;
        private String reason;                  // 삭제 사유
    }

    @Data
    @Builder
    public static class DeliveryInfo {
        private List<Long> deliveredTo;         // 전달 완료된 유저 목록
        private List<ReadInfo> readBy;          // 읽음 처리 정보
        private LocalDateTime lastDeliveryAttempt;
    }

    @Data
    @Builder
    public static class ReadInfo {
        private Long userId;
        private LocalDateTime readAt;
    }

    @Data
    @Builder
    public static class MessageMetrics {
        private Map<String, List<String>> reactions;    // 이모지 반응
        private List<String> mentions;                  // 멘션 정보
        private List<String> hashtags;                  // 해시태그
    }
}