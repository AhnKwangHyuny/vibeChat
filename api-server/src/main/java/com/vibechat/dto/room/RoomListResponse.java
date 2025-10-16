package com.vibechat.dto.room;

import com.vibechat.domain.ChatRoom;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 방 목록 조회용 응답 DTO
 * 
 * 방 목록 화면에 표시할 간소화된 정보만 포함
 * - 무한 스크롤 지원
 * - 최신순/인원순/오래된순 정렬 지원
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomListResponse {

    private Long id;
    
    private String title;
    
    private String description;
    
    private boolean isPrivate;
    
    private List<String> tags;
    
    /**
     * 방 생성자 닉네임
     */
    private String creatorNickname;
    
    /**
     * 현재 참가자 수 (Redis에서 실시간 조회)
     */
    private int participantsCount;
    
    /**
     * 최대 참가자 수 (선택적)
     */
    private Integer maxParticipants;
    
    /**
     * 방 생성 시각 (정렬 기준)
     */
    private LocalDateTime createdAt;

    /**
     * ChatRoom 엔티티를 RoomListResponse DTO로 변환
     * 
     * @param chatRoom 변환할 ChatRoom 엔티티
     * @param participantsCount 현재 참가자 수 (Redis에서 조회)
     * @return 변환된 RoomListResponse DTO
     */
    public static RoomListResponse from(ChatRoom chatRoom, int participantsCount) {
        return RoomListResponse.builder()
            .id(chatRoom.getId())
            .title(chatRoom.getTitle())
            .description(chatRoom.getDescription())
            .isPrivate(chatRoom.isPrivate())
            .tags(chatRoom.getRoomTags() != null 
                ? chatRoom.getRoomTags().stream()
                    .map(roomTag -> roomTag.getTag().getName())
                    .collect(Collectors.toList())
                : List.of())
            .creatorNickname(chatRoom.getCreatedBy() != null 
                ? chatRoom.getCreatedBy().getNickname()
                : "알 수 없음")
            .participantsCount(participantsCount)
            .maxParticipants(null) // TODO: 향후 maxParticipants 필드 추가 시 설정
            .createdAt(chatRoom.getCreatedAt())
            .build();
    }

    /**
     * participantsCount 없이 기본값 0으로 변환
     */
    public static RoomListResponse from(ChatRoom chatRoom) {
        return from(chatRoom, 0);
    }
}

