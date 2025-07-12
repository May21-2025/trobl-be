package com.may21.trobl.oAuth;

import com.may21.trobl._global.Message;
import com.may21.trobl._global.security.JwtTokenUtil;
import com.may21.trobl._global.utility.HeaderExtractor;
import com.may21.trobl.auth.AuthDto;
import com.may21.trobl.auth.jwt.TokenInfo;
import com.may21.trobl.user.UserDto;
import com.may21.trobl.user.domain.User;
import com.may21.trobl.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/oauth")
@RequiredArgsConstructor
public class OAuthController {

    private final KakaoOAuthService kakaoOAuthService;
    private final GoogleOAuthService googleOAuthService;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserService userService;

    @GetMapping("/kakao/callback")
    public ResponseEntity<Message> kakaoCallback(@RequestParam String code, HttpServletRequest request, HttpServletResponse httpServletResponse) {
        String email = kakaoOAuthService.signIn(code);
        User user = userService.getUserByEmail(email);
        String ipAddress = HeaderExtractor.extractIpAddress(request);
        String deviceIfo = HeaderExtractor.extractDeviceInfo(request);
        String deviceId = HeaderExtractor.extractDeviceId(request);
        TokenInfo token =
                jwtTokenUtil.generateAccessAndRefreshToken(user, ipAddress, deviceIfo, deviceId);
        token.tokenToHeaders(httpServletResponse);
        return new ResponseEntity<>(Message.success(new AuthDto.Response(user)), HttpStatus.OK);
    }

    /**
     * Google OAuth - ID Token 방식 (권장)
     * 클라이언트에서 직접 ID Token을 전송
     */
    @GetMapping("/google/id-token")
    public ResponseEntity<Message> googleIdTokenLogin(
            @RequestParam String idToken, 
            HttpServletRequest request, 
            HttpServletResponse httpServletResponse) {
        
        String email = googleOAuthService.getEmailFromIdToken(idToken);
        // TODO: User 서비스에서 이메일로 사용자 찾기/생성 로직 추가 필요
        // User user = userService.findOrCreateByGoogleEmail(email);
        
        String ipAddress = HeaderExtractor.extractIpAddress(request);
        String deviceInfo = HeaderExtractor.extractDeviceInfo(request);
        String deviceId = HeaderExtractor.extractDeviceId(request);
        
        // 임시: 이메일 반환 (실제로는 User 객체 반환)
        return new ResponseEntity<>(Message.success(Map.of("email", email)), HttpStatus.OK);
    }

    /**
     * Google OAuth - Authorization Code 방식
     * 서버에서 코드를 토큰으로 교환
     */
    @GetMapping("/google/callback")
    public ResponseEntity<Message> googleCallback(
            @RequestParam String code, 
            HttpServletRequest request, 
            HttpServletResponse httpServletResponse) {
        
        String email = googleOAuthService.getEmailFromAuthCode(code);
        // TODO: User 서비스에서 이메일로 사용자 찾기/생성 로직 추가 필요
        // User user = userService.findOrCreateByGoogleEmail(email);
        
        String ipAddress = HeaderExtractor.extractIpAddress(request);
        String deviceInfo = HeaderExtractor.extractDeviceInfo(request);
        String deviceId = HeaderExtractor.extractDeviceId(request);
        
        // 임시: 이메일 반환 (실제로는 User 객체 반환)
        return new ResponseEntity<>(Message.success(Map.of("email", email)), HttpStatus.OK);
    }
}
