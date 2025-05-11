package com.may21.trobl.auth.service;

import com.may21.trobl._global.exception.BusinessException;
import com.may21.trobl._global.exception.ExceptionCode;
import com.may21.trobl.auth.AuthDto;
import com.may21.trobl.notification.service.NotificationService;
import com.may21.trobl.user.domain.User;
import com.may21.trobl.user.domain.UserRepository;
import com.may21.trobl.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthorizationServiceImpl implements AuthorizationService {
  private final UserService userService;
  private final NotificationService notificationService;
  private final UserRepository userRepository;
  @Lazy
  private final PasswordEncoder passwordEncoder;

  @Override
  public AuthDto.SignUpResponse signUp(AuthDto.SignUpRequest signUpDto) {
    User user = userService.registerUser(signUpDto);
    notificationService.setNotificationSetting(user);
    return new AuthDto.SignUpResponse(user);
  }

  @Override
  public AuthDto.Response signIn(AuthDto.LoginRequest signRequestDto) {
    String username = signRequestDto.getUsername();
    String password = signRequestDto.getPassword();
    User user =  userRepository.findByUsername(username)
        .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));
    if (!passwordEncoder.matches(password, user.getPassword())) {
      throw new BusinessException(ExceptionCode.INVALID_PASSWORD);
    }
    return new AuthDto.Response(user);
  }
}
