package com.vibechat.dto.user;

public record PresignedUrlRequestDto(
    String fileName,
    String contentType
) {}
