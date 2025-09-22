package com.vibechat.domain;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Map;

public class CustomOAuth2User implements OAuth2User {



    private OAuth2User oauth2User;
    private Long userId;

    public CustomOAuth2User(OAuth2User oauth2User, Long userId) {
        this.oauth2User = oauth2User;
        this.userId = userId;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return oauth2User.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return oauth2User.getAuthorities();
    }

    @Override
    public String getName() {
        return oauth2User.getName();
    }

    public Long getUserId() {
        return userId;
    }
}
