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
    @Size(min = 1, max = 5, message = "A room must have between 1 and 5 tags.")
    private List<String> tags;
}
