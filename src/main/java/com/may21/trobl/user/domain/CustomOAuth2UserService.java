package com.may21.trobl.user.domain;

import com.may21.trobl.user.service.UserService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final UserService userService;

    public CustomOAuth2UserService(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName =
                userRequest
                        .getClientRegistration()
                        .getProviderDetails()
                        .getUserInfoEndpoint()
                        .getUserNameAttributeName();

        Map<String, Object> attributes = oAuth2User.getAttributes();

        OAuthUserInfo userInfo = OAuthUserInfo.of(registrationId, userNameAttributeName, attributes);

        return saveOrUpdateUser(userInfo);
    }

    private User saveOrUpdateUser(OAuthUserInfo userInfo) {
        Optional<User> existingUser = userRepository.findByEmail(userInfo.getEmail());
        return existingUser.orElseGet(() -> userService.createOAuthUser(userInfo));
    }
}
