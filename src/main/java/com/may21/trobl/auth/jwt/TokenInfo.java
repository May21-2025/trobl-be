package com.may21.trobl.auth.jwt;

import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;

@Data
public class TokenInfo {
    private String grantType;
    private String accessToken;
    private String refreshToken;

    public TokenInfo(String bearer, String accessToken, String refreshToken) {
        this.grantType = bearer;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public void tokenToHeaders(HttpServletResponse response) {
        response.addHeader("Authorization", "Bearer " + getAccessToken());
        response.addHeader("Refresh-Token", getRefreshToken());
        response.addHeader("Access-Control-Expose-Headers", "Authorization, Refresh-Token");
    }
}
