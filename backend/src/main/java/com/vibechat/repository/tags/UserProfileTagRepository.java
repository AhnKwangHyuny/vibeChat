package com.vibechat.repository.tags;

import com.vibechat.domain.User;
import com.vibechat.domain.tags.Tag;
import com.vibechat.domain.tags.UserProfileTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProfileTagRepository extends JpaRepository<UserProfileTag, Long> {

    List<UserProfileTag> findByUser(User user);

    Optional<UserProfileTag> findByUserAndTag(User user, Tag tag);

    void deleteByUserAndTag(User user, Tag tag);
}
