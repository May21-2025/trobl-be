package com.may21.trobl.auth.service;

import com.may21.trobl.auth.AuthDto;
import com.may21.trobl.user.UserDto;
import com.may21.trobl.user.domain.User;

public interface AuthorizationService {
    AuthDto.SignUpResponse registerUser(AuthDto.SignUpRequest signUpDto);

    boolean checkIfUserUnregisteredIn30days(String email);

    boolean confirmSignUp(String code, String email);

    boolean resendConfirmationCode(String email);

    UserDto.InfoDetail signIn(AuthDto.LoginRequest signRequestDto);

    boolean changePassword(AuthDto.ChangePasswordRequest userDto, User user);

    boolean forgotPassword(String email);

    boolean forgotPasswordConfirm(AuthDto.ForgetPasswordRequest cognitoUserDto);

    boolean logout(Long id);

    boolean unregister(Long id);

}
