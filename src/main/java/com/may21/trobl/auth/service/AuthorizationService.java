package com.may21.trobl.auth.service;

import com.may21.trobl.auth.AuthDto;
import com.may21.trobl.user.domain.User;

public interface AuthorizationService {
    AuthDto.SignUpResponse registerAdminUser(AuthDto.SignUpRequest signUpDto);

    boolean checkIfUserUnregisteredIn30days(String email);

    boolean confirmSignUp(String code, String email);

    boolean resendConfirmationCode(String email);

    AuthDto.Response signIn(AuthDto.LoginRequest signRequestDto);

    boolean changePassword(AuthDto.ChangePasswordRequest userDto, User user);

    boolean forgotPassword(String email);

    boolean forgotPasswordConfirm(AuthDto.ForgetPasswordRequest cognitoUserDto);

    boolean logout(Long id);

    boolean unregister(Long id);
}
