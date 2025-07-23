package com.may21.trobl.auth;

import com.may21.trobl.user.UserDto;
import com.may21.trobl.user.domain.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.Map;

public class AuthDto {

    @Getter
    @AllArgsConstructor
    public static class LoginRequest {
        private String username;
        private String password;
        private Map<String, String> oAuthData;
    }


    @Getter
    public static class SignUpResponse {
        private final Long userId;

        public SignUpResponse(User user) {
            this.userId = user.getId();
        }
    }

    @Getter
    @AllArgsConstructor
    public static class SignUpRequest {
        private final String username;
        private final String password;
        private final String nickname;
        private final String address;
        private final Boolean married;
        private final LocalDate marriedDate;
        private final LocalDate marriageDate;
        private final Long partnerId;
        private final Map<String, String> oAuthData;

        public boolean isMarried() {
            return married != null && married;
        }
    }

    @Getter
    public static class Response {
        private final Long userId;
        private final String username;
        private final String nickname;
        private final String thumbnailUrl;

        public Response(User user) {
            this.userId = user.getId();
            this.username = user.getUsername();
            this.nickname = user.getNickname();
            this.thumbnailUrl = user.getThumbnailUrl();
        }
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
    public class ChangePasswordRequest {
        private final String oldPassword;
        private final String newPassword;

        public ChangePasswordRequest(String oldPassword, String newPassword) {
            this.oldPassword = oldPassword;
            this.newPassword = newPassword;
        }
    }

    @Getter
    public class ForgetPasswordRequest {
        private final String email;

        public ForgetPasswordRequest(String email) {
            this.email = email;
        }
    }

    @Getter
    public static class Token {
        private final String accessToken;
        private final String refreshToken;
        private final int expiresIn;
        private final String idToken;

        public Token(String accessToken, String refreshToken, String idToken, int expiresIn) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.expiresIn = expiresIn;
            this.idToken = idToken;
        }
    }

}
