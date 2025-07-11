package com.may21.trobl._global.config;

import com.may21.trobl._global.aop.ApiQueryCounter;
import com.may21.trobl._global.aop.ApiQueryInspector;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HibernateConfig {
    
    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer(ApiQueryCounter apiQueryCounter) {
        return hibernateProperties -> {
            hibernateProperties.put("hibernate.session_factory.statement_inspector", 
                new ApiQueryInspector(apiQueryCounter));
        };
    }
}