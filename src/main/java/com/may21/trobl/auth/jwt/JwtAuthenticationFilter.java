package com.may21.trobl.auth.jwt;

import com.may21.trobl._global.exception.ExceptionCode;
import com.may21.trobl._global.security.JwtTokenUtil;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

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


            if (token == null) {
                request.setAttribute("exception", ExceptionCode.TOKEN_MISSING);
            } else {
                String username = jwtTokenUtil.extractUsername(token);

                if (username == null) {
                    request.setAttribute("exception", ExceptionCode.TOKEN_PARSE_FAILED);
                } else if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    if (Boolean.TRUE.equals(jwtTokenUtil.validateToken(token, userDetails))) {
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails, null, userDetails.getAuthorities());

                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        log.debug("Security Context에 '{}' 인증 정보 설정 완료", username);
                    } else {
                        request.setAttribute("exception", ExceptionCode.INVALID_ACCESS_TOKEN);
                    }
                }
            }

        } catch (ExpiredJwtException e) {
            request.setAttribute("exception", ExceptionCode.ACCESS_TOKEN_EXPIRED);
        } catch (Exception e) {
            //log only when not GET
            if (!request.getMethod().equalsIgnoreCase("GET")) {
                log.error("JWT 필터 오류: {}", e.getMessage());
                request.setAttribute("exception", ExceptionCode.TOKEN_PARSE_FAILED);
            }
        }

        filterChain.doFilter(request, response);
    }
}
