package com.may21.trobl.auth.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.may21.trobl._global.exception.ExceptionCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        ExceptionCode exception = (ExceptionCode) request.getAttribute("exception");
        if (exception == null) {
            exception = ExceptionCode.INVALID_ACCESS_TOKEN;
        }
        response.setStatus(exception.getStatus().value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                Map.of("code", exception.getCode(), "message", exception.getMessage())
        ));
    }
}

