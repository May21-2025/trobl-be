package com.may21.trobl._global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.Collections;

@Configuration
public class GoogleCloudStorageConfig {

    @Value("${GOOGLE_CLOUD_PROJECT_ID}")
    private String projectId;

    @Value("${GCP_CREDENTIALS_PATH}")
    private String credentialsPath;

    @Bean
    public Storage storage() throws IOException {
        GoogleCredentials credentials = GoogleCredentials
                .fromStream(new ClassPathResource(credentialsPath).getInputStream())
                .createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));

        return StorageOptions.newBuilder()
                .setProjectId(projectId)
                .setCredentials(credentials)
                .build()
                .getService();
    }
}