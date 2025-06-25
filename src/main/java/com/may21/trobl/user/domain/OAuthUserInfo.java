package com.may21.trobl.user.domain;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
public class OAuthUserInfo {
    private final String provider;
    private final String providerId;
    private final String email;

    @Builder
    public OAuthUserInfo(String provider, String providerId, String email) {
        this.provider = provider;
        this.providerId = providerId;
        this.email = email;
    }

    public static OAuthUserInfo of(
            String registrationId, String userNameAttributeName, Map<String, Object> attributes) {
        if ("kakao".equals(registrationId)) {
            return ofKakao(userNameAttributeName, attributes);
        } else if ("apple".equals(registrationId)) {
            return ofApple(userNameAttributeName, attributes);
        }

        return ofGoogle(userNameAttributeName, attributes);
    }

    private static OAuthUserInfo ofGoogle(
            String userNameAttributeName, Map<String, Object> attributes) {
        return OAuthUserInfo.builder()
                .provider("google")
                .providerId(attributes.get(userNameAttributeName).toString())
                .email((String) attributes.get("email"))
                .build();
    }

    private static OAuthUserInfo ofKakao(
            String userNameAttributeName, Map<String, Object> attributes) {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

        return OAuthUserInfo.builder()
                .provider("kakao")
                .providerId(attributes.get(userNameAttributeName).toString())
                .email((String) kakaoAccount.get("email"))
                .build();
    }

    private static OAuthUserInfo ofApple(
            String userNameAttributeName, Map<String, Object> attributes) {
        // Apple은 name을 제공하지 않을 수 있으므로, 이메일 앞부분을 닉네임으로 사용
        String email = (String) attributes.get("email");
        String name = email != null ? email.split("@")[0] : "Apple User";

        return OAuthUserInfo.builder()
                .provider("apple")
                .providerId(attributes.get(userNameAttributeName).toString())
                .email(email)
                .build();
    }
}
