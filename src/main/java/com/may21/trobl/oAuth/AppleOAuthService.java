package com.may21.trobl.oAuth;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Value;
import com.may21.trobl._global.exception.BusinessException;
import com.may21.trobl._global.exception.ExceptionCode;
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
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Map;

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

    @Value("${APPLE_REDIRECT_URI}")
    private String APPLE_REDIRECT_URI;


    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private PublicKey getApplePublicKey(String keyId) throws Exception {
        String response = restTemplate.getForObject(APPLE_KEYS_URL, String.class);
        JsonNode keysJson = objectMapper.readTree(response);

        for (JsonNode key : keysJson.get("keys")) {
            if (keyId.equals(key.get("kid")
                    .asText())) {
                return buildPublicKey(key);
            }
        }

        throw new RuntimeException("Apple public key not found for keyId: " + keyId);
    }

    private PublicKey buildPublicKey(JsonNode key) throws Exception {
        String n = key.get("n")
                .asText();
        String e = key.get("e")
                .asText();

        byte[] nBytes = Base64.getUrlDecoder()
                .decode(n);
        byte[] eBytes = Base64.getUrlDecoder()
                .decode(e);

        BigInteger modulus = new BigInteger(1, nBytes);
        BigInteger exponent = new BigInteger(1, eBytes);

        RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
        KeyFactory factory = KeyFactory.getInstance("RSA");

        return factory.generatePublic(spec);
    }

    private String extractEmail(Claims claims) {
        return claims.get("email", String.class);
    }


    public String getEmailFromAppleToken(String identityToken) {
        try {
            String[] chunks = identityToken.split("\\.");
            String header = new String(Base64.getUrlDecoder()
                    .decode(chunks[0]), StandardCharsets.UTF_8);
            JsonNode headerJson = objectMapper.readTree(header);
            String keyId = headerJson.get("kid")
                    .asText();

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