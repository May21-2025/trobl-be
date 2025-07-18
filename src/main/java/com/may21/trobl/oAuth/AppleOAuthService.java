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
import com.nimbusds.jose.Algorithm;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AppleOAuthService {

    private static final String APPLE_KEYS_URL = "https://appleid.apple.com/auth/keys";
    private static final String APPLE_ISSUER = "https://appleid.apple.com";
    @Value("${APPLE_CLIENT_ID}")
    private String APPLE_CLIENT_ID;
    @Value("${APPLE_TEAM_ID}")
    private String APPLE_TEAM_ID;

    @Value("${APPLE_KEY_ID}")
    private String APPLE_KEY_ID;

    @Value("${APPLE_KEY_PATH}")
    private String APPLE_KEY_PATH;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

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
                    .verifyWith(publicKey)
                    .requireIssuer(APPLE_ISSUER)
                    .requireAudience(APPLE_CLIENT_ID)
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