package com.vibechat.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportRequest {

    @NotNull
    private ReportReason reason;

    @Size(max = 255)
    private String details;

    public enum ReportReason {
        SPAM, ABUSE, NSFW, OTHER
    }
}
