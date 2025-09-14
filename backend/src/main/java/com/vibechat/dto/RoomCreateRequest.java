package com.vibechat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RoomCreateRequest {

    @NotBlank
    @Size(max = 80)
    private String title;

    @Size(max = 255)
    private String description;

    @NotNull
    private Boolean isPrivate;

    @NotNull
    @Size(min = 1, max = 5, message = "반드시 1개 이상 5개 이하의 태그가 채팅방에 존재해야 합니다.")
    private List<String> tags;
}
