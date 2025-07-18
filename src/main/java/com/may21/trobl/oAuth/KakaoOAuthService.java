package com.may21.trobl.oAuth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.may21.trobl._global.exception.BusinessException;
import com.may21.trobl._global.exception.ExceptionCode;
import com.may21.trobl.auth.AuthDto;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class KakaoOAuthService {

    private final ObjectMapper objectMapper;

    @Value("${KAKAO_CLIENT_ID}")
    String KAKAO_CLIENT_ID;

    @Value("${KAKAO_REDIRECT_URI}")
    String KAKAO_REDIRECT_URI;

    @Transactional
    public String signIn(String code) {
        AuthDto.Token tokenDto = getAccessTokenByCode(code);
        String email = getUserEmailFromAccessToken(tokenDto.getAccessToken());
        return email;
    }

    public AuthDto.Token getAccessTokenByCode(String code) {
        HttpEntity<MultiValueMap<String, String>> kakaoTokenRequest = getMultiValueMapHttpEntity(code);
        RestTemplate rt = new RestTemplate();
        ResponseEntity<String> response =
                rt.exchange(
                        "https://kauth.kakao.com/oauth/token",
                        HttpMethod.POST,
                        kakaoTokenRequest,
                        String.class);
        String responseBody = response.getBody();
        try {
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            return new AuthDto.Token(
                    jsonNode.get("access_token").asText(),
                    jsonNode.get("refresh_token").asText(),
                    null,
                    jsonNode.get("refresh_token_expires_in").asInt());
        } catch (JsonProcessingException e) {
            throw new BusinessException(ExceptionCode.OAUTH2_AUTHORIZATION_INVALID);
        }
    }

    private HttpEntity<MultiValueMap<String, String>> getMultiValueMapHttpEntity(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        String kakaoRedirectUri = KAKAO_REDIRECT_URI;
        body.add("grant_type", "authorization_code");
        body.add("client_id", KAKAO_CLIENT_ID);
        body.add("redirect_uri", kakaoRedirectUri);
        body.add("code", code);
        // HTTP 요청 보내기
        return new HttpEntity<>(body, headers);
    }


    public String getUserEmailFromAccessToken(String accessToken) {
        // HTTP Header 생성
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");


        // HTTP 요청 보내기
        HttpEntity<MultiValueMap<String, String>> kakaoUserInfoRequest = new HttpEntity<>(headers);
        RestTemplate rt = new RestTemplate();
        ResponseEntity<String> response =
                rt.exchange(
                        "https://kapi.kakao.com/v2/user/me",
                        HttpMethod.POST,
                        kakaoUserInfoRequest,
                        String.class);


        String responseBody = response.getBody();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            return jsonNode.get("kakao_account").get("email").asText();
        } catch (JsonProcessingException e) {
            throw new BusinessException(ExceptionCode.OAUTH2_AUTHORIZATION_INVALID);
        }
    }

    public void unregister(String kakaoRefreshToken) {
        AuthDto.Token tokenDto = reissueKakaoToken(kakaoRefreshToken);
        String accessToken = tokenDto.getAccessToken();
        if (accessToken != null) unlinkOauth(accessToken);
    }


    public HttpEntity<MultiValueMap<String, String>> kakaoTokenHeaderMaker(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");
        // HTTP 요청 보내기
        return new HttpEntity<>(headers);
    }


    public void unlinkOauth(String accessToken) {
        HttpEntity<MultiValueMap<String, String>> kakaoTokenRequest =
                kakaoTokenHeaderMaker(accessToken);
        RestTemplate rt = new RestTemplate();
        ResponseEntity<String> response =
                rt.exchange(
                        "https://kapi.kakao.com/v1/user/unlink",
                        HttpMethod.GET,
                        kakaoTokenRequest,
                        String.class);
        String responseBody = response.getBody();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            long id = jsonNode.get("id").asLong();
        } catch (JsonProcessingException e) {
            log.error("Kakao Unlink Failed : {}", e.getMessage());
        }
    }


    public AuthDto.Token reissueKakaoToken(String refreshToken) {
        // need to implement
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");
        RestTemplate rt = new RestTemplate();
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("client_id", KAKAO_CLIENT_ID);
        body.add("refresh_token", refreshToken);
        HttpEntity<MultiValueMap<String, String>> kakaoTokenRequest = new HttpEntity<>(body, headers);
        ResponseEntity<String> response =
                rt.exchange(
                        "https://kauth.kakao.com/oauth/token",
                        HttpMethod.POST,
                        kakaoTokenRequest,
                        String.class);
        String responseBody = response.getBody();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            String kakaoAccessToken = jsonNode.get("access_token").asText();
            String kakaoRefreshToken =
                    Optional.ofNullable(jsonNode.get("refresh_token"))
                            .map(JsonNode::asText)
                            .orElse(refreshToken);
            Integer kakaoRefreshTokenExpiresIn =
                    Optional.ofNullable(jsonNode.get("refresh_token_expires_in"))
                            .map(JsonNode::asInt)
                            .orElse(null);


            return new AuthDto.Token(
                    kakaoAccessToken,
                    kakaoRefreshToken,
                    null,
                    kakaoRefreshTokenExpiresIn == null ? 0 : kakaoRefreshTokenExpiresIn);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ExceptionCode.OAUTH2_AUTHORIZATION_INVALID);
        }
    }

}
