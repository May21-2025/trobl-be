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
    private String email;
  }

  @Getter
  public class EmailConfirm {
    private final String email;
    private final String code;

    public EmailConfirm(String email, String code) {
      this.email = email;
      this.code = code;
    }
  }

  @Getter
  public static class Response {
    private Long userId;
    private String username;
    private String nickname;

    public Response(User user) {
      this.userId = user.getId();
      this.username = user.getUsername();
      this.nickname = user.getNickname();
    }
  }

  public class ChangePasswordRequest {
    private final String oldPassword;
    private final String newPassword;

    public ChangePasswordRequest(String oldPassword, String newPassword) {
      this.oldPassword = oldPassword;
      this.newPassword = newPassword;
    }
  }

  public class ForgetPasswordRequest {
    private final String email;

    public ForgetPasswordRequest(String email) {
      this.email = email;
    }
  }
}
