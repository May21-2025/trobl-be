package com.may21.trobl.oAuth;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.may21.trobl._global.enums.OAuthProvider;
import com.may21.trobl._global.exception.BusinessException;
import com.may21.trobl._global.exception.ExceptionCode;
import com.may21.trobl.user.domain.User;
import com.may21.trobl.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class GoogleOAuthService {
    private final UserRepository userRepository;

    @Value("${GOOGLE_CLIENT_ID}")
    String googleClientId;

    @Value("${GOOGLE_SECRET}")
    String googleClientSecret;


    String userinfoUrl = "https://www.googleapis.com/oauth2/v1/userinfo";
    String revokeUri = "https://oauth2.googleapis.com/revoke";


    public String getEmailFromGoogleToken(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(headers);
        ResponseEntity<String> userInfoResponse =
                new RestTemplate().exchange(userinfoUrl, HttpMethod.GET, request, String.class);
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(userInfoResponse.getBody());
            String email = root.get("email").asText();
            if (email == null) throw new BusinessException(ExceptionCode.GOOGLE_USERINFO_INVALID);
            return email;
        } catch (Exception e) {
            throw new BusinessException(ExceptionCode.GOOGLE_USERINFO_INVALID);
        }
    }

    public String revokeGoogleOAuthWithAccessToken(String accessToken) {
        RestTemplate rt = new RestTemplate();
        String revokeTokenUrl = revokeUri + "?token=" + accessToken;
        ResponseEntity<String> response =
                rt.exchange(revokeTokenUrl, HttpMethod.POST, null, String.class);
        return response.getBody();
    }

    public String getEmailFromAuthToken(String serverAuthCode) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/x-www-form-urlencoded");
        String body = "code=" + serverAuthCode +
                      "&client_id=" + googleClientId +
                      "&client_secret=" + googleClientSecret +
                      "&grant_type=authorization_code";
        HttpEntity<String> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response =
                restTemplate.exchange("https://oauth2.googleapis.com/token", HttpMethod.POST, request, String.class);

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(response.getBody());
            String accessToken = root.get("access_token").asText();
            return getEmailFromGoogleToken(accessToken);
        } catch (Exception e) {
            throw new BusinessException(ExceptionCode.GOOGLE_USERINFO_INVALID);
        }
    }
}

