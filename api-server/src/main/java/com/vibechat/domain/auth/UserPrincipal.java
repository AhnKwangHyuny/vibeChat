package com.vibechat.domain.auth;

import com.vibechat.domain.UserProvider;

import java.io.Serializable;

public final class UserPrincipal implements Serializable {
    private static final long serialVersionUID = 1L; // Serializable 클래스에 대한 serialVersionUID 추가

    private final Long id;
    private final String nickname;
    private final UserProvider provider;

    public UserPrincipal(Long id, String nickname, UserProvider provider) {
        this.id = id;
        this.nickname = nickname;
        this.provider = provider;
    }

    public Long id() {
        return id;
    }

    public String nickname() {
        return nickname;
    }

    public UserProvider provider() {
        return provider;
    }

    @Override
    public String toString() {
        return "UserPrincipal[" +
                "id=" + id + ", " +
                "nickname='" + nickname + "\'" + ", " +
                "provider=" + provider +
                ']';
    }
}

