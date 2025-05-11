package com.may21.trobl.auth.service;

import com.may21.trobl.auth.AuthDto;
import com.may21.trobl.user.domain.User;

public interface AuthorizationService {
  AuthDto.SignUpResponse signUp(AuthDto.SignUpRequest signUpDto);

  AuthDto.Response signIn(AuthDto.LoginRequest signRequestDto);
}
