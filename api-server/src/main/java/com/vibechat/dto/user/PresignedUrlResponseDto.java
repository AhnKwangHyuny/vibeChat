package com.vibechat.dto.user;

public record PresignedUrlResponseDto(
    String presignedUrl,
    String imageUrl
) {}
