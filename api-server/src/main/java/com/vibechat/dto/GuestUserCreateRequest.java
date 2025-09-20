package com.vibechat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GuestUserCreateRequest {
    @NotBlank
    @Size(min = 2, max = 32)
    private String nickname;
}
