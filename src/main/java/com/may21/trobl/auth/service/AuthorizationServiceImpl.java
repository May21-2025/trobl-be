package com.may21.trobl.auth.service;

import com.may21.trobl._global.enums.OAuthProvider;
import com.may21.trobl._global.exception.BusinessException;
import com.may21.trobl._global.exception.ExceptionCode;
import com.may21.trobl.auth.AuthDto;
import com.may21.trobl.notification.service.NotificationService;
import com.may21.trobl.oAuth.AppleOAuthService;
import com.may21.trobl.oAuth.GoogleOAuthService;
import com.may21.trobl.user.UserDto;
import com.may21.trobl.user.domain.User;
import com.may21.trobl.user.domain.UserRepository;
import com.may21.trobl.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthorizationServiceImpl implements AuthorizationService {
    private final UserService userService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final GoogleOAuthService googleOAuthService;
    private final AppleOAuthService appleOAuthService;
    @Lazy
    private final PasswordEncoder passwordEncoder;

    @Override
    public AuthDto.SignUpResponse registerUser(AuthDto.SignUpRequest signUpDto) {
        User user = userService.registerUser(signUpDto);
        userService.updateUserProfile(user, signUpDto);
        notificationService.setNotificationSetting(user);
        return new AuthDto.SignUpResponse(user);
    }

    @Override
    public boolean checkIfUserUnregisteredIn30days(String email) {
        throw new BusinessException(ExceptionCode.NOT_IMPLEMENTED);
    }

    @Override
    public boolean confirmSignUp(String code, String email) {
        throw new BusinessException(ExceptionCode.NOT_IMPLEMENTED);
    }

    @Override
    public boolean resendConfirmationCode(String email) {
        throw new BusinessException(ExceptionCode.NOT_IMPLEMENTED);
    }

    @Override
    public UserDto.InfoDetail signIn(AuthDto.LoginRequest signRequestDto) {
        String username = signRequestDto.getUsername();
        String password = signRequestDto.getPassword();
        Map<String, String> oAuthData = signRequestDto.getOAuthData();
        User user = null;
        if (username == null && (oAuthData != null && !oAuthData.isEmpty())) {
            String provider = oAuthData.get("provider");
            if (provider == null || provider.isEmpty()) {
                throw new BusinessException(ExceptionCode.INVALID_INPUT_VALUE);
            }
            OAuthProvider oAuthProvider = OAuthProvider.fromString(provider);
            switch (oAuthProvider) {
                case GOOGLE -> {
                    String serverAuthCode = oAuthData.get("serverAuthCode");
                    if (serverAuthCode == null || serverAuthCode.isEmpty()) {
                        throw new BusinessException(ExceptionCode.INVALID_INPUT_VALUE);
                    }
                    username = googleOAuthService.getEmailFromAuthToken(serverAuthCode);
                }
                case APPLE -> {
                    String identityToken = oAuthData.get("identityToken");
                    if (identityToken == null || identityToken.isEmpty()) {
                        throw new BusinessException(ExceptionCode.INVALID_INPUT_VALUE);
                    }
                    username = appleOAuthService.getEmailFromAppleToken(identityToken);
                }
            }
            user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));
        } else {
            user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));
            if (!passwordEncoder.matches(password, user.getPassword())) {
                throw new BusinessException(ExceptionCode.INVALID_PASSWORD);
            }
        }

        Long partnerId = user.getPartnerId();
        User partner = partnerId != null ? userRepository.findById(partnerId).orElse(null) : null;
        return new UserDto.InfoDetail(user, partner);
    }

    @Override
    public boolean changePassword(AuthDto.ChangePasswordRequest userDto, User user) {
        throw new BusinessException(ExceptionCode.NOT_IMPLEMENTED);
    }

    @Override
    public boolean forgotPassword(String email) {
        throw new BusinessException(ExceptionCode.NOT_IMPLEMENTED);
    }

    @Override
    public boolean forgotPasswordConfirm(AuthDto.ForgetPasswordRequest cognitoUserDto) {
        throw new BusinessException(ExceptionCode.NOT_IMPLEMENTED);
    }

    @Override
    public boolean logout(Long id) {
        throw new BusinessException(ExceptionCode.NOT_IMPLEMENTED);
    }

    @Override
    public boolean unregister(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));
        OAuthProvider type = user.getAttribute("OAuth") != null ? OAuthProvider.valueOf(user.getAttribute("OAuth")) : OAuthProvider.NONE;
        unlinkOAuth(type, user);
        userRepository.delete(user);
        return true;
    }

    public void unlinkOAuth(OAuthProvider type, User user) {
        if (type == null || user == null) {
            throw new BusinessException(ExceptionCode.INVALID_INPUT_VALUE);
        }
        switch (type) {
            case GOOGLE -> user.setPartner(null);
            case APPLE -> user.setPartner(null);
            case NONE -> throw new BusinessException(ExceptionCode.INVALID_INPUT_VALUE);
        }
        user.setPartner(null);
    }
}
