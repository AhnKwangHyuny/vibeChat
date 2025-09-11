package com.vibechat.repository;

import com.vibechat.domain.User;
import com.vibechat.domain.UserProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByProviderAndProviderId(UserProvider provider, String providerId);
    boolean existsByNickname(String nickname);
}
