package com.vibechat.domain;

public enum UserProvider {
    GOOGLE,
    GUEST;

    public static UserProvider fromString(String value) {
        if (value == null) throw new IllegalArgumentException("provider is null");
        String v = value.trim().toLowerCase();
        return switch (v) {
            case "google" -> GOOGLE;
            case "guest" -> GUEST;
            default -> throw new IllegalArgumentException("Unknown provider: " + value);
        };
    }

    public String toStringValue() {
        return name().toLowerCase();
    }
}


