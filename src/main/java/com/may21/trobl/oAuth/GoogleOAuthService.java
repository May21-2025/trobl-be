package com.may21.trobl.oAuth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.may21.trobl._global.exception.BusinessException;
import com.may21.trobl._global.exception.ExceptionCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

    @Value("${GOOGLE_CLIENT_ID}")
    String googleClientId;

    @Value("${GOOGLE_SECRET}")
    String googleClientSecret;

    @Value("${GOOGLE_REDIRECT_URI}")
    String googleRedirectUri;

    String revokeUri = "https://oauth2.googleapis.com/revoke";
    ObjectMapper objectMapper = new ObjectMapper();


    public String getEmailFromGoogleIdToken(String idToken) {
        try {
            // Google의 tokeninfo endpoint로 ID Token 검증 및 사용자 정보 조회
            String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;

            log.debug("Google ID Token 검증 URL: {}", idToken);
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new BusinessException(ExceptionCode.GOOGLE_USERINFO_INVALID,
                        response.getBody());
            }

            // JSON 파싱하여 사용자 정보 추출
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(response.getBody());

            // 에러 체크
            if (root.has("error")) {
                throw new BusinessException(ExceptionCode.GOOGLE_USERINFO_INVALID, root.get("error")
                        .asText());
            }

            String email = root.get("email")
                    .asText();
            if (email == null || email.isEmpty()) {
                throw new BusinessException(ExceptionCode.GOOGLE_USERINFO_INVALID,
                        "Google ID Token에서 이메일을 찾을 수 없습니다.");
            }

            return email;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ExceptionCode.GOOGLE_USERINFO_INVALID, e);
        }
    }

    /**
     * Access Token으로 Google API 호출 (기존 방식 - 호환성 유지)
     */
    public String getEmailFromGoogleAccessToken(String accessToken) {
        String userinfoUrl = "https://www.googleapis.com/oauth2/v1/userinfo";
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(headers);
        ResponseEntity<String> userInfoResponse =
                new RestTemplate().exchange(userinfoUrl, HttpMethod.GET, request, String.class);
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(userInfoResponse.getBody());
            String email = root.get("email")
                    .asText();
            if (email == null) throw new BusinessException(ExceptionCode.GOOGLE_USERINFO_INVALID,
                    "Google UserInfo API에서 이메일을 찾을 수 없습니다.");
            return email;
        } catch (Exception e) {
            throw new BusinessException(ExceptionCode.GOOGLE_USERINFO_INVALID, e);
        }
    }

    /**
     * ID Token 방식으로 이메일 추출 (권장)
     * 클라이언트에서 직접 ID Token을 받아서 처리
     */
    public String getEmailFromIdToken(String idToken) {
        return getEmailFromGoogleIdToken(idToken);
    }

    /**
     * Authorization Code로 토큰 교환 후 ID Token에서 이메일 추출
     */
    public String getEmailFromAuthCode(String serverAuthCode) {
        if (serverAuthCode == null || serverAuthCode.trim()
                .isEmpty()) {
            throw new IllegalArgumentException("Server auth code cannot be null or empty");
        }

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", serverAuthCode);
        params.add("client_id", googleClientId);
        params.add("client_secret", googleClientSecret);
        params.add("redirect_uri", googleRedirectUri);
        params.add("grant_type", "authorization_code");

        // Log parameters (excluding sensitive data)
        log.info("OAuth token exchange - client_id: {}, redirect_uri: {}, grant_type: {}",
                googleClientId, googleRedirectUri, "authorization_code");
        log.debug("Authorization code length: {}", serverAuthCode.length());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<String> response =
                    restTemplate.exchange("https://oauth2.googleapis.com/token", HttpMethod.POST,
                            request, String.class);

            log.info("Successfully exchanged auth code for token");
            JsonNode root = objectMapper.readTree(response.getBody());
            String accessToken = root.get("access_token")
                    .asText();
            if (accessToken != null && !accessToken.isEmpty()) {
                return getEmailFromGoogleAccessToken(accessToken);
            }
            String idToken = root.get("id_token")
                    .asText();
            if (idToken != null && !idToken.isEmpty()) {
                return getEmailFromGoogleIdToken(idToken);
            }
        } catch (Exception e) {
            log.error("Unexpected error during OAuth token exchange", e);
            throw new RuntimeException("OAuth token exchange failed due to unexpected error", e);
        }
        return null;
    }


/**
 * Google OAuth 토큰 해제
 */
public String revokeGoogleOAuth(String token) {
    RestTemplate rt = new RestTemplate();
    String revokeTokenUrl = revokeUri + "?token=" + token;
    ResponseEntity<String> response =
            rt.exchange(revokeTokenUrl, HttpMethod.POST, null, String.class);
    return response.getBody();
}

public String getEmailFromIdentityToken(String idToken) {
    try {
        // Google의 tokeninfo endpoint로 ID Token 검증 및 사용자 정보 조회
        String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;

        log.debug("Google ID Token 검증 URL: {}", idToken);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new BusinessException(ExceptionCode.GOOGLE_USERINFO_INVALID, response.getBody());
        }

        // JSON 파싱하여 사용자 정보 추출
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode root = objectMapper.readTree(response.getBody());

        // 에러 체크
        if (root.has("error")) {
            throw new BusinessException(ExceptionCode.GOOGLE_USERINFO_INVALID, root.get("error")
                    .asText());
        }

        String email = root.get("email")
                .asText();
        if (email == null || email.isEmpty()) {
            throw new BusinessException(ExceptionCode.GOOGLE_USERINFO_INVALID,
                    "Google ID Token에서 이메일을 찾을 수 없습니다.");
        }

        return email;

    } catch (BusinessException e) {
        throw e;
    } catch (Exception e) {
        throw new BusinessException(ExceptionCode.GOOGLE_USERINFO_INVALID, e);
    }
}
    }