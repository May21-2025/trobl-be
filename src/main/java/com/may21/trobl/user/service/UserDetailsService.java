package com.may21.trobl.user.service;

import com.may21.trobl.user.domain.User;
import com.may21.trobl.user.domain.CustomUserDetails;
import com.may21.trobl.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsService {
  private final UserRepository userRepository;

  public UserDetails loadUserByUsername(String targetUsername) {

    User user =
        userRepository
            .findByUsername(targetUsername)
            .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + targetUsername));
    return new CustomUserDetails(user);
  }
}
