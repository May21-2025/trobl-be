package com.may21.trobl._global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.may21.trobl.storage.CdnCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
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
        Map<String, String> info = new HashMap<>();
        ServiceAccountCredentials saCreds = (ServiceAccountCredentials) credentials;
        info.put("email", saCreds.getClientEmail());
        info.put("projectId", saCreds.getProjectId());
        info.put("type", "Service Account");
        log.info(info.toString());
        return StorageOptions.newBuilder()
                .setProjectId(projectId)
                .setCredentials(credentials)
                .build()
                .getService();
    }

    @Bean
    public GoogleCredentials googleCredentials() throws IOException {
        return GoogleCredentials
                .fromStream(new ClassPathResource(credentialsPath).getInputStream())
                .createScoped(List.of(
                        "https://www.googleapis.com/auth/cloud-platform",
                        "https://www.googleapis.com/auth/compute"
                ));
    }
}