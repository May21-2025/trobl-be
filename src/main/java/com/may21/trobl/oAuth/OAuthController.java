package com.may21.trobl.oAuth;

import com.may21.trobl._global.Message;
import com.may21.trobl._global.exception.BusinessException;
import com.may21.trobl._global.exception.ExceptionCode;
import com.may21.trobl._global.security.JwtTokenUtil;
import com.may21.trobl._global.utility.HeaderExtractor;
import com.may21.trobl.auth.AuthDto;
import com.may21.trobl.auth.jwt.TokenInfo;
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

@RestController
@RequestMapping("/oauth")
@RequiredArgsConstructor
public class OAuthController {

    private final KakaoOAuthService kakaoOAuthService;
    private final GoogleOAuthService googleOAuthService;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserService userService;
    private final AppleOAuthService appleOAuthService;

    @GetMapping("/kakao/callback")
    public ResponseEntity<Message> kakaoCallback(@RequestParam String code,
            HttpServletRequest request, HttpServletResponse httpServletResponse) {
        String email = kakaoOAuthService.signIn(code);
        User user = userService.getUserByEmail(email);
        if (null == user) throw new BusinessException(ExceptionCode.USER_NOT_FOUND);
        String ipAddress = HeaderExtractor.extractIpAddress(request);
        String deviceIfo = HeaderExtractor.extractDeviceInfo(request);
        String deviceId = HeaderExtractor.extractDeviceId(request);
        TokenInfo token =
                jwtTokenUtil.generateAccessAndRefreshToken(user, ipAddress, deviceIfo, deviceId);
        token.tokenToHeaders(httpServletResponse);
        return new ResponseEntity<>(Message.success(token), HttpStatus.OK);
    }


    @GetMapping("/apple/callback")
    public ResponseEntity<Message> appleCallback(@RequestParam String code,
            HttpServletRequest request, HttpServletResponse httpServletResponse) {
        String username = appleOAuthService.getEmailFromAppleToken(code);
        User user = userService.getUserByEmail(username);
        if (null == user) throw new BusinessException(ExceptionCode.USER_NOT_FOUND);

        String ipAddress = HeaderExtractor.extractIpAddress(request);
        String deviceInfo = HeaderExtractor.extractDeviceInfo(request);
        String deviceId = HeaderExtractor.extractDeviceId(request);
        TokenInfo token =
                jwtTokenUtil.generateAccessAndRefreshToken(user, ipAddress, deviceInfo, deviceId);
        token.tokenToHeaders(httpServletResponse);
        return new ResponseEntity<>(Message.success(token), HttpStatus.OK);
    }

    /**
     * Google OAuth - Authorization Code 방식
     * 서버에서 코드를 토큰으로 교환
     */
    @GetMapping("/google/callback")
    public ResponseEntity<Message> googleCallback(@RequestParam String code,
            HttpServletRequest request, HttpServletResponse httpServletResponse) {

        String username = googleOAuthService.getEmailFromAuthCode(code);
        User user = userService.getUserByEmail(username);
        if (null == user) throw new BusinessException(ExceptionCode.USER_NOT_FOUND);

        String ipAddress = HeaderExtractor.extractIpAddress(request);
        String deviceInfo = HeaderExtractor.extractDeviceInfo(request);
        String deviceId = HeaderExtractor.extractDeviceId(request);
        TokenInfo token =
                jwtTokenUtil.generateAccessAndRefreshToken(user, ipAddress, deviceInfo, deviceId);
        token.tokenToHeaders(httpServletResponse);
        return new ResponseEntity<>(Message.success(token), HttpStatus.OK);
    }
}
