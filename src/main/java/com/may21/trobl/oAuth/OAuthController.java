package com.may21.trobl.oAuth;

import com.may21.trobl._global.Message;
import com.may21.trobl._global.security.JwtTokenUtil;
import com.may21.trobl._global.utility.HeaderExtractor;
import com.may21.trobl.auth.AuthDto;
import com.may21.trobl.auth.jwt.TokenInfo;
import com.may21.trobl.user.UserDto;
import com.may21.trobl.user.domain.User;
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
    private final JwtTokenUtil jwtTokenUtil;

    @GetMapping("/kakao/callback")
    public ResponseEntity<Message> kakaoCallback(@RequestParam String code, HttpServletRequest request, HttpServletResponse httpServletResponse) {
        User user = kakaoOAuthService.signIn(code);
        String ipAddress = HeaderExtractor.extractIpAddress(request);
        String deviceIfo = HeaderExtractor.extractDeviceInfo(request);
        String deviceId = HeaderExtractor.extractDeviceId(request);
        TokenInfo token =
                jwtTokenUtil.generateAccessAndRefreshToken(user, ipAddress, deviceIfo, deviceId);
        token.tokenToHeaders(httpServletResponse);
        return new ResponseEntity<>(Message.success(new AuthDto.Response(user)), HttpStatus.OK);
    }
}
