package com.may21.trobl.user.service;

import com.may21.trobl._global.enums.OAuthProvider;
import com.may21.trobl._global.enums.RoleType;
import com.may21.trobl._global.enums.TargetType;
import com.may21.trobl._global.exception.BusinessException;
import com.may21.trobl._global.exception.ExceptionCode;
import com.may21.trobl.auth.AuthDto;
import com.may21.trobl.oAuth.AppleOAuthService;
import com.may21.trobl.oAuth.GoogleOAuthService;
import com.may21.trobl.report.ReportDto;
import com.may21.trobl.report.ReportService;
import com.may21.trobl.user.UserDto;
import com.may21.trobl.user.domain.OAuthUserInfo;
import com.may21.trobl.user.domain.User;
import com.may21.trobl.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private final UserRepository userRepository;
    @Lazy
    private final PasswordEncoder passwordEncoder;
    private final GoogleOAuthService googleOAuthService;
    private final AppleOAuthService appleOAuthService;
    private final ReportService reportService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository
                .findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));
    }

    @Transactional
    public User registerUser(AuthDto.SignUpRequest signUpDto) {
        String username = signUpDto.getUsername();
        String password = signUpDto.getPassword();
        String nickname = signUpDto.getNickname();

        Map<String, String> oAuthData = signUpDto.getOAuthData();
        if (oAuthData != null && !oAuthData.isEmpty()) {
            String provider = oAuthData.get("provider");
            if (username == null && (provider == null || provider.isEmpty())) {
                throw new BusinessException(ExceptionCode.INVALID_INPUT_VALUE);
            }
            OAuthProvider oAuthProvider = OAuthProvider.fromString(provider);
            switch (oAuthProvider) {
                case GOOGLE -> {
                    String accessToken = oAuthData.get("accessToken");
                    String idToken = oAuthData.get("idToken");
                    if (accessToken == null || accessToken.isEmpty()) {
                        throw new BusinessException(ExceptionCode.INVALID_INPUT_VALUE);
                    }
                    username = googleOAuthService.getEmailFromGoogleToken(accessToken);
                }
                case APPLE -> {
                    String identityToken = oAuthData.get("identityToken");
                    if (identityToken == null || identityToken.isEmpty()) {
                        throw new BusinessException(ExceptionCode.INVALID_INPUT_VALUE);
                    }
                    username = appleOAuthService.getEmailFromAppleToken(identityToken);
                }
            }
        }

        if (userRepository.existsByUsername(username)) {
            throw new BusinessException(ExceptionCode.USERNAME_ALREADY_EXISTS);
        }
        User user =
                User.builder()
                        .username(username)
                        .encryptPassword(passwordEncoder.encode(password))
                        .nickname(nickname)
                        .role(RoleType.ADMIN)
                        .build();
        return userRepository.save(user);
    }

    @Transactional
    public void incrementFailedLoginAttempts(String username) {
        User user =
                userRepository
                        .findByUsername(username)
                        .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));

        userRepository.incrementFailedLoginAttempts(username);

        // 최대 실패 횟수 초과 시 계정 잠금
        if (user.getFailedLoginAttempts() + 1 >= MAX_FAILED_ATTEMPTS) {
            userRepository.updateAccountLockStatus(username, false);
            log.warn("계정이 잠겼습니다. 사용자: {}", username);
        }
    }

    @Transactional
    public void resetFailedLoginAttempts(String username) {
        userRepository.resetFailedLoginAttempts(username);
    }

    @Transactional
    public void changePassword(String username, String newPassword) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));
        user.updatePassword(passwordEncoder.encode(newPassword));
    }

    @Transactional
    public UserDto.Info updateUserLanguage(Long userId, String language) {
        throw new BusinessException(ExceptionCode.NOT_IMPLEMENTED);
    }

    public UserDto.Info getUserData(Long userId) {
        throw new BusinessException(ExceptionCode.NOT_IMPLEMENTED);
    }

    public UserDto.InfoDetail getUserInfoDetail(Long userId) {
        throw new BusinessException(ExceptionCode.NOT_IMPLEMENTED);
    }

    @Transactional
    public UserDto.Info updateUserProfile(Long userId, UserDto.Request request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));
        user.update(request);
        return new UserDto.Info(user);
    }

    public UserDto.AlertSetting getEmailAlarmStatus(Long userId) {
        throw new BusinessException(ExceptionCode.NOT_IMPLEMENTED);
    }

    @Transactional
    public void setEmailAlarmStatus(UserDto.AlertSetting request, Long userId) {
        throw new BusinessException(ExceptionCode.NOT_IMPLEMENTED);
    }

    @Transactional
    public User createOAuthUser(OAuthUserInfo userInfo) {
        String username = userInfo.getProviderId();
        String email = userInfo.getEmail();
        String provider = userInfo.getProvider();

        if (userRepository.existsByUsername(username)) {
            throw new BusinessException(ExceptionCode.USERNAME_ALREADY_EXISTS);
        }
        User user =
                User.builder()
                        .username(username)
                        .encryptEmail(passwordEncoder.encode(email))
                        .provider(provider)
                        .nickname("user1")
                        .role(RoleType.USER)
                        .build();
        return userRepository.save(user);
    }

    public UserDto.NotificationSetting getUsersNotification(Long id) {
        throw new BusinessException(ExceptionCode.NOT_IMPLEMENTED);
    }

    @Transactional
    public UserDto.NotificationSetting updateNotificationSetting(Long id, String type, boolean enabled) {
        throw new BusinessException(ExceptionCode.NOT_IMPLEMENTED);
    }

    public boolean isNicknameAvailable(String nickname) {
        if (nickname == null || nickname.isEmpty()) {
            throw new BusinessException(ExceptionCode.INVALID_INPUT_VALUE);
        }
        if (nickname.length() > 10) {
            throw new BusinessException(ExceptionCode.NICKNAME_REQUIREMENTS_NOT_MET);
        }
        return !userRepository.existsByNickname(nickname);
    }

    @Transactional
    public boolean updateNickname(Long userId, String nickname) {
        if (nickname == null || nickname.isEmpty()) {
            throw new BusinessException(ExceptionCode.INVALID_INPUT_VALUE);
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));
        if (user.getNicknameUpdatedAt().plusDays(30).isAfter(LocalDate.now())) {
            throw new BusinessException(ExceptionCode.NICKNAME_UPDATE_RESTRICTED);
        }

        if (Objects.equals(user.getNickname(), nickname)) {
            throw new BusinessException(ExceptionCode.NICKNAME_EQUAL_TO_EXISTING);
        }
        if (nickname.length() > 10) {
            throw new BusinessException(ExceptionCode.NICKNAME_REQUIREMENTS_NOT_MET);
        }
        if (userRepository.existsByNickname(nickname)) {
            throw new BusinessException(ExceptionCode.NICKNAME_ALREADY_EXISTS);
        }
        user.updateNickname(nickname);
        return true;
    }

    @Transactional
    public boolean updateInformation(Long userId, UserDto.InfoRequest requestBody) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));
        user.updateInformation(requestBody);
        return true;
    }

    @Transactional
    public boolean matchPartner(Long id, String partnerId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));
        User partner = userRepository.findByUsername(partnerId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));

        if (user.getPartnerId() != null || partner.getPartnerId() != null) {
            throw new BusinessException(ExceptionCode.RESTRICTED);
        }

        user.setPartner(partner);
        partner.setPartner(user);
        return true;
    }

    public User createUser(String email, OAuthProvider oAuthProvider) {
        if (email == null || email.isEmpty()) {
            throw new BusinessException(ExceptionCode.INVALID_INPUT_VALUE);
        }
        if (userRepository.existsByUsername(email)) {
            throw new BusinessException(ExceptionCode.USERNAME_ALREADY_EXISTS);
        }
        User user =
                User.builder()
                        .username(email)
                        .encryptEmail(passwordEncoder.encode(email))
                        .provider(oAuthProvider.name())
                        .nickname("")
                        .role(RoleType.USER)
                        .build();
        return userRepository.save(user);
    }

    public boolean unregister(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));

        userRepository.delete(user);
        return true;
    }

    public User getUserByEmail(String email) {
        if (email == null || email.isEmpty()) {
            throw new BusinessException(ExceptionCode.INVALID_INPUT_VALUE);
        }
        return userRepository.findByUsername(email)
                .orElse(null);
    }

    public boolean reportUser(Long userId, Long targetId, ReportDto.Request reportRequest) {
        if (userId.equals(targetId)) {
            throw new BusinessException(ExceptionCode.FORBIDDEN);
        }
        if (reportRequest.getReportType() == null) {
            throw new BusinessException(ExceptionCode.INVALID_INPUT_VALUE);
        }
        return reportService.report(userId, targetId, TargetType.USER, reportRequest) > 0;

    }
}
