package com.may21.trobl._global.security;

import static com.may21.trobl._global.utility.HeaderExtractor.extractDeviceId;
import static com.may21.trobl._global.utility.HeaderExtractor.extractRefreshToken;

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
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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

  /** JWT 토큰에서 사용자 이름 추출 */
  public String extractUsername(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  /** JWT 토큰에서 만료 시간 추출 */
  public Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

  /** JWT 토큰에서 특정 클레임 추출 */
  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  /** JWT 토큰에서 모든 클레임 추출 */
  private Claims extractAllClaims(String token) {
    SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY_JWT.getBytes(StandardCharsets.UTF_8));

    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }

  private Boolean isTokenExpired(String token) {
    final Date expiration = extractExpiration(token);
    return expiration.before(new Date());
  }

  /** 사용자 정보로 Access Token 생성 */
  public String generateToken(UserDetails userDetails, String deviceId, long duration) {
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    return createToken(authentication, deviceId, duration);
  }

  /** JWT 토큰 생성 공통 메서드 */
  public String createToken(Authentication authentication, String deviceId, long expireTime) {
    String authorities =
        authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.joining(","));
    Date now = new Date();
    Date accessExpiration = new Date(now.getTime() + expireTime);
    Long userId = ((User) authentication.getPrincipal()).getId();

    return Jwts.builder()
        .subject(authentication.getName())
        .claim("auth", authorities)
        .claim("userId", userId)
        .claim("device_id", deviceId)
        .issuedAt(now)
        .expiration(accessExpiration)
        .signWith(getKey(), Jwts.SIG.HS256)
        .compact();
  }
  public String generateRefreshToken(Long userId, String deviceId, String parentTokenId) {
    String tokenId = UUID.randomUUID().toString();
    Instant expiryDate = Instant.now().plus(14, ChronoUnit.DAYS);

    byte[] keyBytes = SECRET_KEY_JWT.getBytes(StandardCharsets.UTF_8);
    Key key = new SecretKeySpec(keyBytes,Jwts.SIG.HS512.toString());

    String token = Jwts.builder()
            .subject(userId.toString())
            .id(tokenId)
            .issuedAt(new Date())
            .expiration(Date.from(expiryDate))
            .signWith(key)
            .compact();
    RefreshToken refreshToken = RefreshToken.builder()
            .tokenId(tokenId)
            .userId(userId)
            .deviceId(deviceId)
            .expiryDate(expiryDate)
            .parentId(parentTokenId)
            .build();
    refreshTokenRepository.save(refreshToken);
    return token;
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

  /** 토큰 검증 */
  public Boolean validateToken(String token, UserDetails userDetails) {
    try {
      final String username = extractUsername(token);
      return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    } catch (ExpiredJwtException e) {
      log.error("JWT 토큰이 만료되었습니다: {}", e.getMessage());
      return false;
    } catch (UnsupportedJwtException e) {
      log.error("지원되지 않는 JWT 토큰입니다: {}", e.getMessage());
      return false;
    } catch (MalformedJwtException e) {
      log.error("잘못된 형식의 JWT 토큰입니다: {}", e.getMessage());
      return false;
    } catch (IllegalArgumentException e) {
      log.error("JWT 클레임 문자열이 비어있습니다: {}", e.getMessage());
      return false;
    }
  }

  private Claims getClaims(String jwt) {
    return Jwts.parser().verifyWith(getKey()).build().parseSignedClaims(jwt).getPayload();
  }

  /** HTTP 요청에서 JWT 토큰 추출 */
  public String getTokenFromRequest(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7);
    }
    throw new BusinessException(ExceptionCode.INVALID_ACCESS_TOKEN);
  }

  public TokenInfo createTokenInfo(Authentication authentication, String deviceId) {
    UserDetails userDetails;
    if (authentication != null
        && authentication.getPrincipal() instanceof UserDetails extractedUserDetail) {
      userDetails = extractedUserDetail;
    } else if (authentication != null && authentication.getPrincipal() instanceof User user) {
      userDetails = user;
    } else {
      throw new IllegalArgumentException("Invalid authentication object");
    }
    String accessToken = generateToken(userDetails, deviceId, ACCESS_TOKEN_EXPIRATION);
    String refreshToken = generateToken(userDetails, deviceId, REFRESH_TOKEN_EXPIRATION);

    return new TokenInfo("Bearer", accessToken, refreshToken);
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
    Claims refreshTokenClaims = getClaims(parentRefreshToken);
    String parentTokenId = refreshTokenClaims.get("id").toString();
    String deviceId = extractDeviceId(request);
    String refreshToken = generateRefreshToken(user.getId(),deviceId,parentTokenId);
    String accessToken = generateToken(user, deviceId, ACCESS_TOKEN_EXPIRATION);
    return new TokenInfo("Bearer", accessToken, refreshToken);
  }

  public TokenInfo generateAccessAndRefreshToken(
      AuthDto.Response authDto, String ipAddress, String deviceInfo, String deviceId) {
    User user = new User(authDto.getUserId(), authDto.getUsername(), "", new ArrayList<>());
    String refreshToken = generateToken(user, deviceId, REFRESH_TOKEN_EXPIRATION);
    String accessToken = generateToken(user, deviceId, ACCESS_TOKEN_EXPIRATION);
    createRefreshToken(refreshToken, user.getId(), ipAddress, deviceId, deviceInfo);
    return new TokenInfo("Bearer", accessToken, refreshToken);
  }

  public void createRefreshToken(
      String refreshTokenValue, Long userId, String ipAddress, String deviceId, String deviceInfo) {
    Optional<RefreshToken> existingToken =
        refreshTokenRepository.findByUserIdAndDeviceInfo(userId, deviceInfo);

    // 기존 토큰이 있다면 폐기 처리
    existingToken.ifPresent(
        token -> {
          token.setRevoked(true);
          refreshTokenRepository.save(token);
        });

    String tokenHash = hashToken(refreshTokenValue);
    // 토큰 만료 시간 설정 (예: 7일)
    Instant expiryDate = Instant.now().plus(7, ChronoUnit.DAYS);

    RefreshToken refreshToken =
        RefreshToken.builder()
            .token(tokenHash)
            .userId(userId)
            .expiryDate(expiryDate)
            .deviceId(deviceId)
            .ipAddress(ipAddress)
            .build();

    refreshTokenRepository.save(refreshToken);
  }

  private String hashToken(String token) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new BusinessException(ExceptionCode.TOKEN_PARSE_FAILED, e);
    }
  }
}
