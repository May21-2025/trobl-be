package com.may21.trobl.user.service;

import com.may21.trobl._global.enums.RoleType;
import com.may21.trobl._global.exception.BusinessException;
import com.may21.trobl._global.exception.ExceptionCode;
import com.may21.trobl.auth.AuthDto;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

  private static final int MAX_FAILED_ATTEMPTS = 5;
  private final UserRepository userRepository;
  @Lazy private final PasswordEncoder passwordEncoder;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    return userRepository
        .findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));
  }

  @Transactional
  public User registerAdminUser(AuthDto.SignUpRequest signUpDto) {
    String username = signUpDto.getUsername();
    String password = signUpDto.getPassword();
    String nickname = signUpDto.getNickname();
    String email = signUpDto.getEmail();

    if (userRepository.existsByUsername(username)) {
      throw new BusinessException(ExceptionCode.USERNAME_ALREADY_EXISTS);
    }
    User user =
        User.builder()
            .username(username)
            .encryptEmail(passwordEncoder.encode(email))
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
    userRepository.updatePassword(
        username, passwordEncoder.encode(newPassword), System.currentTimeMillis());
  }

  public UserDto.Info updateUserLanguage(Long userId, String language) {
    throw new BusinessException(ExceptionCode.NOT_IMPLEMENTED);
  }

  public UserDto.Info getUserData(Long userId) {
    throw new BusinessException(ExceptionCode.NOT_IMPLEMENTED);
  }

  public UserDto.InfoDetail getUserInfoDetail(Long userId) {
    throw new BusinessException(ExceptionCode.NOT_IMPLEMENTED);
  }

  public UserDto.Info updateUserProfile(UserDto.InfoRequest userRequestDto, Long userId) {
    throw new BusinessException(ExceptionCode.NOT_IMPLEMENTED);
  }

  public UserDto.AlertSetting getEmailAlarmStatus(Long userId) {
    throw new BusinessException(ExceptionCode.NOT_IMPLEMENTED);
  }

  public void setEmailAlarmStatus(UserDto.AlertSetting request, Long userId) {
    throw new BusinessException(ExceptionCode.NOT_IMPLEMENTED);
  }

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
}
