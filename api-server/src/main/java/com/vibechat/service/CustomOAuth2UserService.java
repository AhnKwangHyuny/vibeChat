package com.vibechat.service;

import com.vibechat.domain.CustomOAuth2User;
import com.vibechat.domain.User;
import com.vibechat.domain.UserProvider;
import com.vibechat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        UserProvider up = UserProvider.fromString(registrationId);

        String providerId = oauth2User.getName(); // Google's sub claim
        // String email = oauth2User.getAttribute("email"); // 필요 시 사용
        String name = oauth2User.getAttribute("name");
        String picture = oauth2User.getAttribute("picture");

        Optional<User> existingUser = userRepository.findByProviderAndProviderId(up, providerId);

        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
            user.updateProfileFromOAuth(name, picture);
        } else {
            user = User.createOAuth(up, providerId, name, picture);
        }
        User savedUser = userRepository.save(user);

        return new CustomOAuth2User(oauth2User, savedUser.getId());
    }
}
