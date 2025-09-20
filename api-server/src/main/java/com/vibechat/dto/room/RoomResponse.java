package com.vibechat.dto.room;

import com.vibechat.domain.ChatRoom;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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

    /**
     * ChatRoom 엔티티를 RoomResponse DTO로 변환하는 정적 팩토리 메서드입니다.
     * @param chatRoom 변환할 ChatRoom 엔티티
     * @return 변환된 RoomResponse DTO
     */
    public static RoomResponse from(ChatRoom chatRoom) {
        RoomResponse response = new RoomResponse();
        response.setId(chatRoom.getId());
        response.setTitle(chatRoom.getTitle());
        response.setDescription(chatRoom.getDescription());
        response.setPrivate(chatRoom.isPrivate());
        response.setInviteCode(chatRoom.getInviteCode());

        if (chatRoom.getRoomTags() != null) {
            response.setTags(chatRoom.getRoomTags().stream()
                    .map(roomTag -> roomTag.getTag().getName())
                    .collect(Collectors.toList()));
        }

        return response;
    }
}