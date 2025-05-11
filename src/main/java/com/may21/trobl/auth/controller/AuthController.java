package com.may21.trobl.auth.controller;

import com.may21.trobl._global.Message;
import com.may21.trobl._global.security.JwtTokenUtil;
import com.may21.trobl._global.utility.HeaderExtractor;
import com.may21.trobl.auth.AuthDto;
import com.may21.trobl.auth.jwt.TokenInfo;
import com.may21.trobl.auth.service.AuthorizationService;
import com.may21.trobl.user.domain.User;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

  private final JwtTokenUtil jwtTokenUtil;
  private final AuthorizationService authorizationService;

  @PostMapping("/sign-up")
  public ResponseEntity<Message> createUser(@RequestBody AuthDto.SignUpRequest signUpDto) {
    AuthDto.SignUpResponse response = authorizationService.signUp(signUpDto);
    return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
  }

}
