package com.may21.trobl.oAuth;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Value;
import com.may21.trobl._global.exception.BusinessException;
import com.may21.trobl._global.exception.ExceptionCode;
import com.may21.trobl._global.security.JwtTokenUtil;
import com.may21.trobl.auth.AuthDto;
import com.may21.trobl.auth.jwt.TokenInfo;
import com.may21.trobl.user.domain.RefreshToken;
import com.may21.trobl.user.domain.RefreshTokenRepository;
import com.may21.trobl.user.domain.User;
import com.may21.trobl.user.domain.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AppleOAuthService {

    private static final String APPLE_KEYS_URL = "https://appleid.apple.com/auth/keys";
    private static final String APPLE_ISSUER = "https://appleid.apple.com";

    @Value("${APPLE_CLIENT_ID}")
    private String appleClientId;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;


    public TokenInfo authenticateWithApple(String identityToken, String deviceId, String ipAddress, String deviceInfo) {
        try {
            String email = verifyAppleToken(identityToken);

            // 2. 사용자 조회 또는 생성
            User user = userRepository.findByEmail(email).orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));

            // 3. AuthDto.Response 생성 (기존 구조에 맞춤)
            AuthDto.Response authDto = new AuthDto.Response(user);

            // 4. 기존 JwtTokenUtil 사용해서 토큰 생성
            TokenInfo tokenInfo = jwtTokenUtil.generateAccessAndRefreshToken(
                    user, ipAddress, deviceInfo, deviceId);

            saveRefreshToken(user.getId(), tokenInfo.getRefreshToken(), deviceId, ipAddress, deviceInfo);

            return tokenInfo;

        } catch (Exception e) {
            log.error("Apple authentication failed", e);
            throw new BusinessException(ExceptionCode.AUTHENTICATION_FAILED);
        }
    }

    private String verifyAppleToken(String identityToken) {
        try {
            // 1. 토큰 헤더에서 kid(Key ID) 추출
            String[] chunks = identityToken.split("\\.");
            String header = new String(Base64.getUrlDecoder().decode(chunks[0]), StandardCharsets.UTF_8);
            JsonNode headerJson = objectMapper.readTree(header);
            String keyId = headerJson.get("kid").asText();

            // 2. Apple 공개키 가져오기
            PublicKey publicKey = getApplePublicKey(keyId);

            // 3. jjwt를 사용한 토큰 검증
            Claims claims = Jwts.parser()
                    .verifyWith((RSAPublicKey) publicKey)
                    .requireIssuer(APPLE_ISSUER)
                    .requireAudience(appleClientId)
                    .build()
                    .parseSignedClaims(identityToken)
                    .getPayload();


            return extractEmail(claims);

        } catch (Exception e) {
            log.error("Apple token verification failed", e);
            throw new BusinessException(ExceptionCode.INVALID_REQUEST);
        }
    }

    private PublicKey getApplePublicKey(String keyId) throws Exception {
        String response = restTemplate.getForObject(APPLE_KEYS_URL, String.class);
        JsonNode keysJson = objectMapper.readTree(response);

        for (JsonNode key : keysJson.get("keys")) {
            if (keyId.equals(key.get("kid").asText())) {
                return buildPublicKey(key);
            }
        }

        throw new RuntimeException("Apple public key not found for keyId: " + keyId);
    }

    private PublicKey buildPublicKey(JsonNode key) throws Exception {
        String n = key.get("n").asText();
        String e = key.get("e").asText();

        byte[] nBytes = Base64.getUrlDecoder().decode(n);
        byte[] eBytes = Base64.getUrlDecoder().decode(e);

        BigInteger modulus = new BigInteger(1, nBytes);
        BigInteger exponent = new BigInteger(1, eBytes);

        RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
        KeyFactory factory = KeyFactory.getInstance("RSA");

        return factory.generatePublic(spec);
    }

    private String extractEmail(Claims claims) {
        return claims.get("email", String.class);
    }

    private void saveRefreshToken(Long userId, String refreshToken, String deviceId,
                                  String ipAddress, String deviceInfo) {
        try {
            // 기존 토큰들 무효화
            List<RefreshToken> existingTokens = refreshTokenRepository.findValidTokenByUserIdAndDeviceId(userId, deviceId);
            existingTokens.forEach(token -> token.setRevoked(true));

            // 새 refresh token 저장
            String hashedToken = JwtTokenUtil.hashToken(refreshToken);
            RefreshToken newRefreshToken = RefreshToken.builder()
                    .userId(userId)
                    .token(hashedToken)
                    .deviceId(deviceId)
                    .ipAddress(ipAddress)
                    .expiryDate(Instant.now().plus(30, ChronoUnit.DAYS))
                    .build();

            refreshTokenRepository.save(newRefreshToken);

        } catch (Exception e) {
            log.error("Failed to save refresh token", e);
            throw new BusinessException(ExceptionCode.MALFORMED_TOKEN);
        }
    }

    public String getEmailFromAppleToken(String identityToken) {
        try {
            String[] chunks = identityToken.split("\\.");
            String header = new String(Base64.getUrlDecoder().decode(chunks[0]), StandardCharsets.UTF_8);
            JsonNode headerJson = objectMapper.readTree(header);
            String keyId = headerJson.get("kid").asText();

            PublicKey publicKey = getApplePublicKey(keyId);

            Claims claims = Jwts.parser()
                    .verifyWith((RSAPublicKey) publicKey)
                    .requireIssuer(APPLE_ISSUER)
                    .requireAudience(appleClientId)
                    .build()
                    .parseSignedClaims(identityToken)
                    .getPayload();

            return extractEmail(claims);

        } catch (Exception e) {
            log.error("Failed to extract email from Apple token", e);
            throw new BusinessException(ExceptionCode.MALFORMED_TOKEN);
        }
    }
}