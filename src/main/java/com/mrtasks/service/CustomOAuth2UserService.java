package com.mrtasks.service;

import com.mrtasks.model.User;
import com.mrtasks.model.UserProfile;
import com.mrtasks.repository.UserRepository;
import com.mrtasks.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String googleId = oAuth2User.getAttribute("sub");

        User user = userRepository.findByUsername(googleId)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setUsername(googleId);
                    newUser.setPassword(""); // No password for OAuth users
                    newUser.setRole("ROLE_USER");
                    newUser.setProvider("google");
                    User savedUser = userRepository.save(newUser);

                    UserProfile profile = new UserProfile();
                    profile.setUser(savedUser);
                    profile.setEmail(email);
                    profile.setCompanyName(name);
                    profile.setEmailVerified(true);
                    userProfileRepository.save(profile);

                    return savedUser;
                });

        return oAuth2User;
    }
}