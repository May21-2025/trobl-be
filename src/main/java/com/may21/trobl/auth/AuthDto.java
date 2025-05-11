package com.may21.trobl.auth;

import com.may21.trobl.user.domain.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class AuthDto {

  @Getter
  @AllArgsConstructor
  public static class LoginRequest {
    private String username;
    private String password;
  }
  

  @Getter
  public static class SignUpResponse {
    private Long userId;

    public SignUpResponse(User user) {
      this.userId = user.getId();
    }
  }

  @Getter
  public static class SignUpRequest {
    private String username;
    private String password;
    private String nickname;
  }
}
