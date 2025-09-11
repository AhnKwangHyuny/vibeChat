package com.vibechat.service.user;

import com.vibechat.domain.User;
import com.vibechat.domain.UserProvider;
import com.vibechat.dto.GuestUserCreateRequest;
import com.vibechat.dto.UserResponse;
import com.vibechat.exception.NicknameConflictException;
import com.vibechat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Value("${app.user.default-guest-avatar-url:/static/avatars/guest.png}")
    private String defaultGuestAvatarUrl;

    @Override
    @Transactional
    public UserResponse createGuestUser(GuestUserCreateRequest request) {
        String nickname = request.getNickname();
        if (userRepository.existsByNickname(nickname)) {
            String suggested = suggestNickname(nickname);
            throw new NicknameConflictException("이미 사용 중인 닉네임입니다.", suggested);

        }

        User user = User.createGuest(nickname);
        user.setAvatarUrl(defaultGuestAvatarUrl);
        User saved = userRepository.save(user);

        return createUserResponse(saved.getId() , saved.getNickname() , saved.getAvatarUrl());
    }

    private UserResponse createUserResponse(Long id  , String nickname , String url) {
        UserResponse resp = new UserResponse();
        resp.setUserId(id);
        resp.setNickname(nickname);
        resp.setAvatarUrl(url);

        return resp;
    }

    private String suggestNickname(String base) {
        int suffix = (int)(Math.random() * 900) + 100;
        String candidate = base + suffix;
        if (userRepository.existsByNickname(candidate)) {
            suffix = (int)(Math.random() * 9000) + 1000;
            candidate = base + suffix;
        }
        return candidate;
    }

    @Override
    @Transactional
    public UserResponse createOrUpdateGoogleUser(String providerId, String nickname, String avatarUrl, String email) {
        // Google 사용자 조회
        Optional<User> existingUser = userRepository.findByProviderAndProviderId(UserProvider.GOOGLE, providerId);
        
        User user;
        if (existingUser.isPresent()) {
            // 기존 사용자 업데이트
            user = existingUser.get();
            user.updateProfileFromOAuth(nickname, avatarUrl);
        } else {
            // 새 사용자 생성
            user = User.createOAuth(UserProvider.GOOGLE, providerId, nickname, avatarUrl);
        }
        
        User saved = userRepository.save(user);
        return createUserResponse(saved.getId(), saved.getNickname(), saved.getAvatarUrl());
    }
}


