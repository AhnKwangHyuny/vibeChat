package com.vibechat.repository;

import com.vibechat.domain.User;
import com.vibechat.domain.UserProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByProviderAndProviderId(UserProvider provider, String providerId);
    boolean existsByNickname(String nickname);

    // 특정 시간 이전에 마지막으로 활동한 게스트 사용자들의 ID 목록을 조회
    @Query("SELECT u.id FROM User u WHERE u.provider = :provider AND u.updatedAt < :threshold")
    List<Long> findInactiveUserIdsByProvider(
        @Param("provider") UserProvider provider,
        @Param("threshold") LocalDateTime threshold
    );
}
