package com.vibechat.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_profile_tags",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "tag_id"}))
@Getter
@Setter
public class UserProfileTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id")
    private Tag tag;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}


