package com.may21.trobl.user.domain;

import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@Getter
public class OAuthAttributes {
  private Map<String, Object> attributes;
  private String nameAttributeKey;
  private String email;
  private String providerId;

  @Builder
  public OAuthAttributes(
      Map<String, Object> attributes, String nameAttributeKey, String email, String providerId) {
    this.attributes = attributes;
    this.nameAttributeKey = nameAttributeKey;
    this.email = email;
    this.providerId = providerId;
  }

  public static OAuthAttributes of(
      String registrationId, String userNameAttributeName, Map<String, Object> attributes) {
    if ("kakao".equals(registrationId)) {
      return ofKakao(userNameAttributeName, attributes);
    } else if ("apple".equals(registrationId)) {
      return ofApple(userNameAttributeName, attributes);
    }

    return ofGoogle(userNameAttributeName, attributes);
  }

  private static OAuthAttributes ofGoogle(
      String userNameAttributeName, Map<String, Object> attributes) {
    return OAuthAttributes.builder()
        .email((String) attributes.get("email"))
        .providerId((String) attributes.get(userNameAttributeName))
        .attributes(attributes)
        .nameAttributeKey(userNameAttributeName)
        .build();
  }

  private static OAuthAttributes ofKakao(
      String userNameAttributeName, Map<String, Object> attributes) {
    Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");

    return OAuthAttributes.builder()
        .email((String) kakaoAccount.get("email"))
        .providerId(String.valueOf(attributes.get(userNameAttributeName)))
        .attributes(attributes)
        .nameAttributeKey(userNameAttributeName)
        .build();
  }

  private static OAuthAttributes ofApple(
      String userNameAttributeName, Map<String, Object> attributes) {
    return OAuthAttributes.builder()
        .email((String) attributes.get("email"))
        .providerId((String) attributes.get(userNameAttributeName))
        .attributes(attributes)
        .nameAttributeKey(userNameAttributeName)
        .build();
  }
}
