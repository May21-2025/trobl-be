package com.may21.trobl.auth.controller;

import com.may21.trobl._global.Message;
import com.may21.trobl._global.exception.BusinessException;
import com.may21.trobl._global.exception.ExceptionCode;
import com.may21.trobl._global.security.JwtTokenUtil;
import com.may21.trobl._global.utility.HeaderExtractor;
import com.may21.trobl.auth.AuthDto;
import com.may21.trobl.auth.jwt.TokenInfo;
import com.may21.trobl.auth.service.AuthorizationService;
import com.may21.trobl.user.UserDto;
import com.may21.trobl.user.domain.User;
import com.may21.trobl.user.service.UserService;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtTokenUtil jwtTokenUtil;
    private final AuthorizationService authorizationService;
    private final UserService userService;

    @PostMapping("/sign-up")
    public ResponseEntity<Message> createUser(@RequestBody AuthDto.SignUpRequest signUpDto, HttpServletRequest request,
                                              HttpServletResponse httpResponse) {
        AuthDto.SignUpResponse response = authorizationService.registerUser(signUpDto);
        User user = new User(response.getUserId(), null, "", List.of());
        String ipAddress = HeaderExtractor.extractIpAddress(request);
        String deviceIfo = HeaderExtractor.extractDeviceInfo(request);
        String deviceId = HeaderExtractor.extractDeviceId(request);
        TokenInfo token =
                jwtTokenUtil.generateAccessAndRefreshToken(user, ipAddress, deviceIfo, deviceId);
        token.tokenToHeaders(httpResponse);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/valid-email")
    public ResponseEntity<Message> checkIfUserUnregisteredIn30days(@RequestParam String email) {
        boolean response = authorizationService.checkIfUserUnregisteredIn30days(email);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    // 이메일 코드 확인
    @PostMapping("/confirm")
    public ResponseEntity<Message> confirmEmailVerificationCode(
            @RequestBody AuthDto.EmailConfirm request) {
        boolean response = authorizationService.confirmSignUp(request.getCode(), request.getEmail());
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    // 이메일 재전송
    @PostMapping("/new-code")
    public ResponseEntity<Message> resendEmailVerificationCode(
            @RequestBody AuthDto.EmailConfirm request) {
        boolean response = authorizationService.resendConfirmationCode(request.getEmail());
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    // 로그인
    @PostMapping("/sign-in")
    public ResponseEntity<Message> signIn(
            @RequestBody AuthDto.LoginRequest signRequestDto,
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            UserDto.InfoDetail infoDto = authorizationService.signIn(signRequestDto);

            String ipAddress = HeaderExtractor.extractIpAddress(request);
            String deviceIfo = HeaderExtractor.extractDeviceInfo(request);
            String deviceId = HeaderExtractor.extractDeviceId(request);

            User user = new User(infoDto.getUserId(), infoDto.getUsername(), "", infoDto.getRoles());
            TokenInfo token =
                    jwtTokenUtil.generateAccessAndRefreshToken(user, ipAddress, deviceIfo, deviceId);
            token.tokenToHeaders(response);
            log.info(response.toString());
            return new ResponseEntity<>(Message.success(infoDto), HttpStatus.OK);
        } catch (BusinessException e) {
            if (e.getErrorCode() == ExceptionCode.USER_NOT_FOUND) {
                return new ResponseEntity<>(Message.fail(null, ExceptionCode.USER_NOT_FOUND), HttpStatus.OK);
            }
            throw e;
        }
    }

    @PutMapping("/password")
    public ResponseEntity<Message> changePassword(
            @RequestBody AuthDto.ChangePasswordRequest userDto,
            @RequestHeader("Authorization") String token) {
        User user = jwtTokenUtil.getUserFromValidateAccessToken(token);
        boolean response = authorizationService.changePassword(userDto, user);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/password")
    public ResponseEntity<Message> forgotPassword(@RequestParam(name = "email") String email) {
        boolean response = authorizationService.forgotPassword(email);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PostMapping("/password/confirm")
    public ResponseEntity<Message> forgotPasswordConfirm(
            @RequestBody @Nullable AuthDto.ForgetPasswordRequest cognitoUserDto) {
        boolean response = authorizationService.forgotPasswordConfirm(cognitoUserDto);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/payload/sub")
    public ResponseEntity<String> message() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return new ResponseEntity<>(auth.getName(), HttpStatus.OK);
    }

    @GetMapping("/reissue")
    public ResponseEntity<Message> reissue(HttpServletRequest request, HttpServletResponse response) {
        TokenInfo token = jwtTokenUtil.reissueAccessToken(request);
        token.tokenToHeaders(response);
        return new ResponseEntity<>(Message.success(true), HttpStatus.OK);
    }

    @GetMapping("/logout")
    public ResponseEntity<Message> logout(
            @RequestHeader("Authorization") String token) {
        User user = jwtTokenUtil.getUserFromValidateAccessToken(token);
        boolean response = authorizationService.logout(user.getId());
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @DeleteMapping("/unregister")
    public ResponseEntity<Message> unregisterUser(
            @Param("reason") Integer reason,
            @Param("detail") String detail,
            @RequestHeader("Authorization") String token) {
        User user = jwtTokenUtil.getUserFromValidateAccessToken(token);
        boolean response = authorizationService.unregister(user.getId());
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/check/username")
    public ResponseEntity<Message> checkUsername(@RequestParam String username) {
        boolean isAvailable = !authorizationService.isUsernameTaken(username);
        return new ResponseEntity<>(Message.success(isAvailable), HttpStatus.OK);
    }

    @GetMapping("/check/nickname")
    public ResponseEntity<Message> checkNickname(@RequestParam String nickname) {
        boolean isAvailable = !userService.checkNicknameValid(nickname);
        return new ResponseEntity<>(Message.success(isAvailable), HttpStatus.OK);
    }
}
