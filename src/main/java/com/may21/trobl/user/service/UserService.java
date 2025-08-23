package com.may21.trobl.user.service;

import com.may21.trobl._global.enums.ItemType;
import com.may21.trobl._global.enums.NotificationType;
import com.may21.trobl._global.enums.OAuthProvider;
import com.may21.trobl._global.enums.RoleType;
import com.may21.trobl._global.exception.BusinessException;
import com.may21.trobl._global.exception.ExceptionCode;
import com.may21.trobl._global.utility.ProfanityFilter;
import com.may21.trobl._global.utility.Utility;
import com.may21.trobl.admin.AdminDto;
import com.may21.trobl.auth.AuthDto;
import com.may21.trobl.notification.dto.NotificationDto;
import com.may21.trobl.oAuth.AppleOAuthService;
import com.may21.trobl.oAuth.GoogleOAuthService;
import com.may21.trobl.oAuth.KakaoOAuthService;
import com.may21.trobl.redis.CacheService;
import com.may21.trobl.redis.RedisDto;
import com.may21.trobl.report.ReportDto;
import com.may21.trobl.report.ReportService;
import com.may21.trobl.user.UserDto;
import com.may21.trobl.user.domain.User;
import com.may21.trobl.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.may21.trobl.post.service.PostingServiceImpl.ADMIN_USERNAME;

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
    private final KakaoOAuthService kakaoOAuthService;
    private final ProfanityFilter profanityFilter;
    private final CacheService cacheService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));
    }

    @Transactional
    public User registerUser(AuthDto.SignUpRequest signUpDto) {
        String username = signUpDto.getUsername();
        String password = signUpDto.getPassword() == null ? UUID.randomUUID()
                .toString() : signUpDto.getPassword();
        String nickname = signUpDto.getNickname();
        String provider = null;
        RoleType role = RoleType.ADMIN;
        Map<String, String> oAuthData = signUpDto.getOAuthData();
        if (oAuthData != null && !oAuthData.isEmpty()) {
            provider = oAuthData.get("provider");
            role = RoleType.USER; // Default role for OAuth users
            if (username == null && (provider == null || provider.isEmpty())) {
                throw new BusinessException(ExceptionCode.INVALID_INPUT_VALUE,
                        "OAuth provider is required");
            }
            OAuthProvider oAuthProvider = OAuthProvider.fromString(provider);
            switch (oAuthProvider) {
                case GOOGLE -> {
                    String idToken = oAuthData.get("idToken");
                    if (idToken == null || idToken.isEmpty()) {
                        throw new BusinessException(ExceptionCode.INVALID_INPUT_VALUE,
                                "idToken is required");
                    }
                    username = googleOAuthService.getEmailFromGoogleIdToken(idToken);
                }
                case APPLE -> {
                    String identityToken = oAuthData.get("identityToken");
                    if (identityToken == null || identityToken.isEmpty()) {
                        throw new BusinessException(ExceptionCode.INVALID_INPUT_VALUE,
                                "identityToken is required");
                    }
                    username = appleOAuthService.getEmailFromAppleToken(identityToken);
                }
                case KAKAO -> {
                    String accessToken = oAuthData.get("accessToken");
                    if (accessToken == null || accessToken.isEmpty()) {
                        throw new BusinessException(ExceptionCode.INVALID_INPUT_VALUE,
                                "accessToken is required");
                    }
                    log.debug("Kakao OAuth access token: {}", accessToken);
                    username = kakaoOAuthService.getUserEmailFromAccessToken(accessToken);
                }
            }
        }

        if (username == null) {
            throw new BusinessException(ExceptionCode.INVALID_INPUT_VALUE,
                    "Username cannot be null");
        }
        if (userRepository.existsByUsername(username)) {
            OAuthProvider oAuthProvider = userRepository.getOAuthByUsername(username);
            throw new BusinessException(ExceptionCode.USERNAME_ALREADY_EXISTS,
                    "{ \"provider\" : " + oAuthProvider + "}");
        }
        User user = User.builder()
                .username(username)
                .encryptPassword(passwordEncoder.encode(password))
                .nickname(nickname)
                .role(role)
                .provider(provider)
                .build();
        user.updateAddress(signUpDto.getAddress());
        if (signUpDto.isMarried()) {
            Long partnerId = signUpDto.getPartnerId();
            String partnerUsername = null;
            if (partnerId != null) {
                User partner = userRepository.findById(signUpDto.getPartnerId())
                        .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));
                partnerUsername = partner.getUsername();
            }
            user.updateInformation(
                    new UserDto.MarriedInfo(signUpDto.getMarriageDate(), partnerUsername));
        }
        return userRepository.save(user);
    }

    @Transactional
    public void incrementFailedLoginAttempts(String username) {
        User user = userRepository.findByUsername(username)
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
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));
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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));
        Long partnerId = user.getPartnerId();
        if (partnerId != null) {
            User partner = userRepository.findById(partnerId)
                    .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));
            return new UserDto.InfoDetail(user, partner);
        }
        return new UserDto.InfoDetail(user, null);
    }

    public UserDto.AlertSetting getEmailAlarmStatus(Long userId) {
        throw new BusinessException(ExceptionCode.NOT_IMPLEMENTED);
    }

    @Transactional
    public void setEmailAlarmStatus(UserDto.AlertSetting request, Long userId) {
        throw new BusinessException(ExceptionCode.NOT_IMPLEMENTED);
    }

    public UserDto.NotificationSetting getUsersNotification(Long id) {
        throw new BusinessException(ExceptionCode.NOT_IMPLEMENTED);
    }

    @Transactional
    public UserDto.NotificationSetting updateNotificationSetting(Long id, String type,
            boolean enabled) {
        throw new BusinessException(ExceptionCode.NOT_IMPLEMENTED);
    }

    public boolean isNicknameAvailable(String nickname) {
        if (nickname == null || nickname.isEmpty()) {
            throw new BusinessException(ExceptionCode.NICKNAME_CANNOT_BE_BLANK);
        }
        if (nickname.length() > 10) {
            throw new BusinessException(ExceptionCode.NICKNAME_REQUIREMENTS_NOT_MET);
        }
        return !userRepository.existsByNickname(nickname);
    }

    @Transactional
    public boolean updateNickname(Long userId, String nickname) {
        if (nickname == null || nickname.isEmpty()) {
            throw new BusinessException(ExceptionCode.NICKNAME_CANNOT_BE_BLANK);
        }
        if (profanityFilter.containsProfanity(nickname))
            throw new BusinessException(ExceptionCode.NICKNAME_CANNOT_CONTAIN_PROFANITY);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));
        if (user.getNicknameUpdatedAt()
                .plusDays(30)
                .isAfter(LocalDate.now())) {
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
        cacheService.invalidateUserCache(user.getId());
        return true;
    }

    @Transactional
    public boolean updateInformation(Long userId, UserDto.MarriedInfo requestBody) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));
        user.updateInformation(requestBody);
        cacheService.invalidateUserCache(user.getId());
        return true;
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

    public User getUserByUsername(String username) {
        if (username == null || username.isEmpty()) {
            throw new BusinessException(ExceptionCode.INVALID_INPUT_VALUE);
        }
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));
    }

    public boolean reportUser(Long userId, Long targetId, ReportDto.Request reportRequest) {
        if (userId.equals(targetId)) {
            throw new BusinessException(ExceptionCode.FORBIDDEN);
        }
        if (reportRequest.getReportType() == null) {
            throw new BusinessException(ExceptionCode.INVALID_INPUT_VALUE,
                    "Report type cannot be null");
        }
        return reportService.report(userId, targetId, ItemType.USER, reportRequest) > 0;

    }

    @Transactional
    public UserDto.Info updateUserProfileImage(Long userId, String imageKey) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));

        // Update user with new image key
        user.setThumbnailKey(imageKey);
        cacheService.invalidateUserCache(user.getId());
        return new UserDto.Info(user);
    }

    @Transactional
    public void updateUserProfile(User user, AuthDto.SignUpRequest signUpDto) {
        if (signUpDto.getNickname() != null && !signUpDto.getNickname()
                .isEmpty()) {
            if (Objects.equals(user.getNickname(), signUpDto.getNickname())) return;
            if (userRepository.existsByNickname(signUpDto.getNickname())) {
                throw new BusinessException(ExceptionCode.NICKNAME_ALREADY_EXISTS);
            }
            user.updateNickname(signUpDto.getNickname());
            if (signUpDto.isMarried()) {
                User partner = userRepository.findById(signUpDto.getPartnerId())
                        .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));
                UserDto.MarriedInfo marriedInfo =
                        new UserDto.MarriedInfo(signUpDto.getMarriageDate(), partner.getUsername());
                user.updateInformation(marriedInfo);
            }
            if (signUpDto.getAddress() != null) user.updateAddress(signUpDto.getAddress());
        }
        userRepository.save(user);
        cacheService.invalidateUserCache(user.getId());
    }

    public boolean deleteAllOauth() {
        List<User> users = userRepository.findAllOAuth();
        userRepository.deleteAll(users);
        for (User user : users) cacheService.invalidateUserCache(user.getId());
        return true;
    }

    public boolean checkNicknameValid(String nickname) {
        if (nickname == null || nickname.isEmpty()) {
            throw new BusinessException(ExceptionCode.NICKNAME_CANNOT_BE_BLANK);
        }
        if (nickname.length() > 10) {
            throw new BusinessException(ExceptionCode.NICKNAME_REQUIREMENTS_NOT_MET);
        }
        return !userRepository.existsByNickname(nickname);
    }

    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));
    }

    @Transactional
    public UserDto.Info createVirtualUsers(AuthDto.SignUpRequest signUpRequest) {
        // Generate a random username if exist regenerate
        String nickname = signUpRequest.getNickname();
        if (nickname == null || userRepository.existsByNickname(nickname)) {
            throw new BusinessException(ExceptionCode.INVALID_INPUT_VALUE,
                    "Nickname cannot be null");
        }
        String username = Utility.getRandomString();
        while (userRepository.existsByUsername(username)) {
            username = Utility.getRandomString();
        }

        User user = User.builder()
                .username(username)
                .encryptPassword(passwordEncoder.encode(UUID.randomUUID()
                        .toString()))
                .nickname(nickname)
                .role(RoleType.USER)
                .provider(OAuthProvider.NONE.name())
                .isTestUser(true)
                .address(signUpRequest.getAddress())
                .build();
        userRepository.save(user);

        return new UserDto.Info(user);
    }

    public boolean connectPartners(AdminDto.ConnectPartners request) {
        Long userId = request.getUserId();
        Long partnerId = request.getPartnerId();

        if (userId == null || partnerId == null) {
            throw new BusinessException(ExceptionCode.INVALID_INPUT_VALUE,
                    "User ID and Partner ID cannot be null");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));
        User partner = userRepository.findById(partnerId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));

        if (user.getPartnerId() != null || partner.getPartnerId() != null) {
            throw new BusinessException(ExceptionCode.USER_ALREADY_HAS_PARTNER);
        }

        user.setPartner(partner, request);
        partner.setPartner(user, request);
        UserDto.MarriedInfo marriedInfo =
                new UserDto.MarriedInfo(request.getMarriageDate(), partner.getUsername());
        user.updateInformation(marriedInfo);
        partner.updateInformation(marriedInfo);
        userRepository.saveAll(List.of(user, partner));
        cacheService.invalidateUserCache(user.getId());
        return true;
    }

    public Page<AdminDto.UserDetails> getAllUsers(int page, int size, String sortBy,
            String sortDirection) {
        if (page < 0 || size <= 0) {
            throw new BusinessException(ExceptionCode.INVALID_INPUT_VALUE,
                    "Page and size must be positive integers");
        }
        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "createdAt";
        }
        if (sortDirection == null || sortDirection.isEmpty()) {
            sortDirection = "desc";
        }
        Pageable pageable = Utility.getPageable(page, size, sortBy, sortDirection);

        Page<User> userPage = userRepository.findByTestUserIsFalseAndUnregisteredIsFalse(pageable);
        List<AdminDto.UserDetails> userDetailsList = userPage.getContent()
                .stream()
                .map(AdminDto.UserDetails::new)
                .toList();

        return new PageImpl<>(userDetailsList, pageable, userPage.getTotalElements());
    }

    @Transactional
    public UserDto.Info updateVirtualUsers(Long userId, UserDto.Update request) {
        User user = userRepository.findByIdAndTestUserIsTrue(userId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));

        if (!Objects.equals(user.getNickname(), request.getNickname()) &&
                (request.getNickname() != null && !request.getNickname()
                        .isEmpty())) {
            if (userRepository.existsByNickname(request.getNickname())) {
                throw new BusinessException(ExceptionCode.NICKNAME_ALREADY_EXISTS);
            }
            user.updateNickname(request.getNickname());
        }

        if (request.getAddress() != null) {
            user.updateAddress(request.getAddress());
        }
        cacheService.invalidateUserCache(user.getId());
        return new UserDto.Info(userRepository.save(user));
    }

    public void setThumbnail(Long userId, String imageKey) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));
        if (imageKey == null || imageKey.isEmpty()) {
            throw new BusinessException(ExceptionCode.INVALID_INPUT_VALUE,
                    "Image key cannot be null or empty");
        }
        user.setThumbnailKey(imageKey);
        userRepository.save(user);
    }

    public NotificationDto.UserNotiSetting setNotificationStatus(Long userId,
            NotificationType notificationType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));
        user.toggleNotificationBlock(notificationType);
        userRepository.save(user);
        return new NotificationDto.UserNotiSetting(user);
    }

    public NotificationDto.UserNotiSetting getNotificationSetting(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));
        return new NotificationDto.UserNotiSetting(user);
    }

    public boolean isVirtualUsers(Long userId) {
        return userRepository.isVirtualUser(userId);
    }

    public UserDto.Info getAdminAccount() {
        User user = userRepository.findByUsername(ADMIN_USERNAME)
                .orElse(null);
        if (user == null) {
            String nickname = ADMIN_USERNAME;
            String username = ADMIN_USERNAME;

            user = User.builder()
                    .username(username)
                    .encryptPassword(passwordEncoder.encode(UUID.randomUUID()
                            .toString()))
                    .nickname(nickname)
                    .role(RoleType.USER)
                    .provider(OAuthProvider.NONE.name())
                    .isTestUser(false)
                    .address(null)
                    .build();
            userRepository.save(user);
        }
        return new UserDto.Info(user);
    }

    public UserDto.Info updateAdminAccount(UserDto.Update request) {
        User user = userRepository.findByUsername(ADMIN_USERNAME)
                .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));
        if (!Objects.equals(user.getNickname(), request.getNickname()) &&
                (request.getNickname() != null && !request.getNickname()
                        .isEmpty())) {
            if (userRepository.existsByNickname(request.getNickname())) {
                throw new BusinessException(ExceptionCode.NICKNAME_ALREADY_EXISTS);
            }
            user.updateNickname(request.getNickname());
        }
        cacheService.invalidateUserCache(user.getId());
        return new UserDto.Info(userRepository.save(user));
    }

    public UserDto.Info getVirtualUser(Long userId) {
        RedisDto.UserDto userDto = cacheService.getUserFromCache(userId);
        return new UserDto.Info(userDto);
    }
}
