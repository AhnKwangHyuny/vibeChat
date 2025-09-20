package com.vibechat.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomJoinRequest {

    @Size(min = 2, max = 32)
    private String nickname; // Optional, for guests joining a room

    private String inviteCode; // Optional, for private rooms
}
