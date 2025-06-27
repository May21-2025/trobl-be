package com.may21.trobl._global.security;

import com.may21.trobl._global.exception.BusinessException;
import com.may21.trobl._global.exception.ExceptionCode;
import com.may21.trobl.auth.AuthDto;
import com.may21.trobl.auth.jwt.TokenInfo;
import com.may21.trobl.user.domain.RefreshToken;
import com.may21.trobl.user.domain.RefreshTokenRepository;
import com.may21.trobl.user.domain.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.may21.trobl._global.utility.HeaderExtractor.extractDeviceId;
import static com.may21.trobl._global.utility.HeaderExtractor.extractRefreshToken;

@Slf4j
@Component
public class JwtTokenUtil {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${security.jwt.secret-key}")
    private String SECRET_KEY_JWT;

    @Value("${security.jwt.access-token-expiration}")
    private long ACCESS_TOKEN_EXPIRATION;

    @Value("${security.jwt.refresh-token-expiration}")
    private long REFRESH_TOKEN_EXPIRATION;

    public JwtTokenUtil(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public SecretKey getKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY_JWT.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * JWT 토큰에서 사용자 이름 추출
     */
    public String extractUsername(String token) {
        byte[] decodedKey = Base64.getDecoder().decode(SECRET_KEY_JWT);
        Key restoredKey = Keys.hmacShaKeyFor(decodedKey);
        return Jwts.parser()
                .setSigningKey(restoredKey) // 복호화에 사용할 키
                .build().parseSignedClaims(token).getPayload()
                .getSubject();             // 바로 subject 꺼냄

    }

    /**
     * JWT 토큰에서 만료 시간 추출
     */
    public Date extractExpiration(String token) {

        byte[] decodedKey = Base64.getDecoder().decode(SECRET_KEY_JWT);
        Key restoredKey = Keys.hmacShaKeyFor(decodedKey);
        return Jwts.parser()
                .setSigningKey(restoredKey) // 복호화에 사용할 키
                .build().parseSignedClaims(token).getPayload()
                .getExpiration();
    }

    /**
     * JWT 토큰에서 특정 클레임 추출
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * JWT 토큰에서 모든 클레임 추출
     */
    private Claims extractAllClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY_JWT.getBytes(StandardCharsets.UTF_8));

        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    private Boolean isTokenExpired(String token) {
        final Date expiration = extractExpiration(token);
        return expiration.before(new Date());
    }

    /**
     * RefreshToken로 Access Token 생성
     */
    public String generateTokenFromRefreshToken(User user, RefreshToken refreshToken, String deviceId) {
        Instant expiryDate = Instant.now().plus(ACCESS_TOKEN_EXPIRATION, ChronoUnit.DAYS);
        if (!Objects.equals(deviceId, refreshToken.getDeviceId()))
            throw new BusinessException(ExceptionCode.TOKEN_PARSE_FAILED);
        byte[] decodedKey = Base64.getDecoder().decode(SECRET_KEY_JWT);
        Key restoredKey = Keys.hmacShaKeyFor(decodedKey);
        Long userId = refreshToken.getUserId();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", userId)
                .id(refreshToken.getTokenId())
                .issuedAt(new Date())
                .expiration(Date.from(expiryDate))
                .signWith(restoredKey)
                .compact();
    }

    public RefreshToken generateRefreshToken(User user, String deviceId, String parentTokenId) {
        String tokenId = UUID.randomUUID().toString();
        Instant expiryDate = Instant.now().plus(REFRESH_TOKEN_EXPIRATION, ChronoUnit.DAYS);

        byte[] decodedKey = Base64.getDecoder().decode(SECRET_KEY_JWT);
        Key restoredKey = Keys.hmacShaKeyFor(decodedKey);

        String token = Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .id(tokenId)
                .issuedAt(new Date())
                .expiration(Date.from(expiryDate))
                .signWith(restoredKey)
                .compact();
        RefreshToken refreshToken = RefreshToken.builder()
                .tokenId(tokenId)
                .token(token)
                .userId(user.getId())
                .deviceId(deviceId)
                .expiryDate(expiryDate)
                .parentId(parentTokenId)
                .build();
        refreshTokenRepository.save(refreshToken);
        return refreshToken;
    }

    public User getAuthentication(String token) {
        Claims claims = getClaims(token);

        String auth =
                Optional.ofNullable(claims.get("auth", String.class))
                        .orElseThrow(() -> new RuntimeException("잘못된 토큰입니다."));
        Long userId =
                Optional.ofNullable(claims.get("userId", Long.class))
                        .orElseThrow(() -> new RuntimeException("잘못된 토큰입니다."));

        Collection<GrantedAuthority> authorities =
                Arrays.stream(auth.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        return new User(userId, claims.getSubject(), "", authorities);
    }

    /**
     * 토큰 검증
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (ExpiredJwtException e) {
            log.error("JWT 토큰이 만료되었습니다: {}", e.getMessage());
            throw new BusinessException(ExceptionCode.ACCESS_TOKEN_EXPIRED);
        } catch (UnsupportedJwtException e) {
            log.error("지원되지 않는 JWT 토큰입니다: {}", e.getMessage());
            throw new BusinessException(ExceptionCode.INVALID_ACCESS_TOKEN);
        } catch (MalformedJwtException e) {
            log.error("잘못된 형식의 JWT 토큰입니다: {}", e.getMessage());
            throw new BusinessException(ExceptionCode.MALFORMED_TOKEN);
        } catch (IllegalArgumentException e) {
            log.error("JWT 클레임 문자열이 비어있습니다: {}", e.getMessage());
            throw new BusinessException(ExceptionCode.TOKEN_MISSING);
        }
    }

    private Claims getClaims(String jwt) {
        return Jwts.parser().verifyWith(getKey()).build().parseSignedClaims(jwt).getPayload();
    }

    /**
     * HTTP 요청에서 JWT 토큰 추출
     */
    public String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        throw new BusinessException(ExceptionCode.INVALID_ACCESS_TOKEN);
    }


    public User getUserFromValidateAccessToken(HttpServletRequest request) {
        String token = getTokenFromRequest(request);
        if (token == null) {
            throw new BusinessException(ExceptionCode.TOKEN_MISSING);
        }
        Claims claims = getClaims(token);
        Long userId = claims.get("userId", Long.class);
        String username = claims.getSubject();
        List<GrantedAuthority> authorities =
                Arrays.stream(claims.get("auth", String.class).split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
        return new User(userId, username, "", authorities);
    }

    public TokenInfo reissueAccessToken(HttpServletRequest request) {
        String parentRefreshToken = extractRefreshToken(request);
        User user = getAuthentication(parentRefreshToken);
        RefreshToken preRefreshToken = refreshTokenRepository.findByToken(parentRefreshToken)
                .orElseThrow(() -> new BusinessException(ExceptionCode.TOKEN_PARSE_FAILED));
        preRefreshToken.setRevoked(true);
        String deviceId = extractDeviceId(request);
        RefreshToken refreshToken = generateRefreshToken(user, deviceId, preRefreshToken.getTokenId());
        String accessToken = generateTokenFromRefreshToken(user, refreshToken, deviceId);
        return new TokenInfo("Bearer", accessToken, refreshToken.getToken());
    }

    public TokenInfo generateAccessAndRefreshToken(
            AuthDto.Response authDto, String ipAddress, String deviceInfo, String deviceId) {

        User user = new User(authDto.getUserId(), authDto.getUsername(), "", new ArrayList<>());
        RefreshToken refreshToken = generateRefreshToken(user, deviceId, null);
        String accessToken = generateTokenFromRefreshToken(user, refreshToken, deviceId);
        return new TokenInfo("Bearer", accessToken, refreshToken.getToken());
    }

    public static String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException(ExceptionCode.TOKEN_PARSE_FAILED, e);
        }
    }
}
