package com.may21.trobl._global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
  @Override
  public void addCorsMappings(CorsRegistry registry) {
    String[] allowedOrigins = getEnvironmentSpecificOrigins();
    registry
        .addMapping("/**")
        .allowedOrigins(allowedOrigins)
        .allowedMethods(
            HttpMethod.GET.name(),
            HttpMethod.POST.name(),
            HttpMethod.PUT.name(),
            HttpMethod.DELETE.name(),
            HttpMethod.PATCH.name(),
            HttpMethod.OPTIONS.name())
        .allowedHeaders("Content-Type", "Authorization", "X-Requested-With")
        .exposedHeaders("Authorization", "X-Device-ID")
        .allowCredentials(true)
        .maxAge(3600);
  }

  private String[] getEnvironmentSpecificOrigins() {
    return new String[] {"http://localhost:3000"};
  }

  @Override
  public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
    configurer
        .favorParameter(false) // 쿼리 파라미터로 콘텐츠 타입을 결정하지 않음
        .ignoreAcceptHeader(false) // Accept 헤더를 사용하여 콘텐츠 타입 결정
        .useRegisteredExtensionsOnly(true) // 등록된 미디어 타입만 허용
        .defaultContentType(MediaType.APPLICATION_JSON); // 기본 미디어 타입 설정
  }
}
