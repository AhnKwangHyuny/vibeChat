package com.vibechat.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserProvider provider;

    @Column(length = 128)
    private String providerId;

    @Column(nullable = false, length = 32, unique = true)
    private String nickname;

    @Column(length = 255)
    private String avatarUrl;

    @Column(length = 160)
    private String greeting;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // 도메인 팩토리 메서드: 게스트 유저 생성
    public static User createGuest(String nickname) {
        User user = new User();
        user.provider = UserProvider.GUEST;
        user.nickname = nickname;
        return user;
    }

    // 도메인 팩토리 메서드: OAuth(예: Google) 유저 생성
    public static User createOAuth(UserProvider provider, String providerId, String nickname, String avatarUrl) {
        User user = new User();
        user.provider = provider;
        user.providerId = providerId;
        user.nickname = nickname;
        user.avatarUrl = avatarUrl;
        return user;
    }

    // 도메인 동작: OAuth 프로필로 표시 이름/아바타 갱신
    public void updateProfileFromOAuth(String nickname, String avatarUrl) {
        if (nickname != null && !nickname.isBlank()) {
            this.nickname = nickname;
        }
        if (avatarUrl != null && !avatarUrl.isBlank()) {
            this.avatarUrl = avatarUrl;
        }
    }
}
