package com.may21.trobl.auth.jwt;

import com.may21.trobl._global.security.JwtTokenUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenUtil jwtTokenUtil;
  private final UserDetailsService userDetailsService;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    try {
      String token = jwtTokenUtil.getTokenFromRequest(request);

      if (token != null) {
        String username = jwtTokenUtil.extractUsername(token);

        // 사용자 이름이 있고 현재 인증이 설정되지 않은 경우
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
          UserDetails userDetails = userDetailsService.loadUserByUsername(username);

          // 토큰 유효성 검사
          if (Boolean.TRUE.equals(jwtTokenUtil.validateToken(token, userDetails))) {
            // 인증 객체 생성
            UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());

            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // 보안 컨텍스트에 인증 설정
            SecurityContextHolder.getContext().setAuthentication(authToken);
            log.debug("Security Context에 '{}' 인증 정보 설정 완료", username);
          }
        }
      }
    } catch (Exception e) {
      log.error("사용자 인증을 설정할 수 없습니다: {}", e.getMessage());
    }

    filterChain.doFilter(request, response);
  }
}
