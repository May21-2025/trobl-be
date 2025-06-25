package com.may21.trobl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@EnableJpaRepositories
@EnableJpaAuditing
@EnableScheduling
@EnableTransactionManagement
@EnableWebMvc
@SpringBootApplication
public class TroblApplication {

    public static void main(String[] args) {
        SpringApplication.run(TroblApplication.class, args);
    }
}
