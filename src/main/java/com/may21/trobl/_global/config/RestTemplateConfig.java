package com.may21.trobl._global.config;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Slf4j
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        // 타임아웃 설정
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 연결 타임아웃 5초
        factory.setReadTimeout(10000);   // 읽기 타임아웃 10초

        restTemplate.setRequestFactory(factory);

        // 에러 핸들러 설정
        restTemplate.setErrorHandler(new CustomResponseErrorHandler());

        // 인터셉터 추가 (로깅, 인증 등)
        restTemplate.getInterceptors().add(new LoggingInterceptor());

        return restTemplate;
    }

    // 커스텀 에러 핸들러
    public static class CustomResponseErrorHandler extends DefaultResponseErrorHandler {
        @Override
        public void handleError(ClientHttpResponse response) throws IOException {
            // 커스텀 에러 처리 로직
            HttpStatusCode statusCode = response.getStatusCode();
            log.error("HTTP Error: {} - {}", statusCode, response.getStatusText());
            super.handleError(response);
        }
    }

    // 로깅 인터셉터
    public static class LoggingInterceptor implements ClientHttpRequestInterceptor {
        private static final Logger log = LoggerFactory.getLogger(LoggingInterceptor.class);

        @Override
        public ClientHttpResponse intercept(
                HttpRequest request,
                byte[] body,
                ClientHttpRequestExecution execution) throws IOException {

            log.debug("HTTP Request: {} {}", request.getMethod(), request.getURI());

            ClientHttpResponse response = execution.execute(request, body);

            log.debug("HTTP Response: {}", response.getStatusCode());

            return response;
        }
    }
}

