package com.may21.trobl._global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.util.Locale;

@Configuration
@EnableScheduling
public class NotificationConfig {

    @Bean
    @Primary
    public LocaleResolver customLocaleResolver() {
        SessionLocaleResolver resolver = new SessionLocaleResolver();
        resolver.setDefaultLocale(Locale.KOREAN);
        return resolver;
    }

    // TaskExecutor와 TaskScheduler는 AsyncConfig에서 관리하도록 이동
    // 더 세밀한 비동기 설정을 위해 AsyncConfig 사용
}