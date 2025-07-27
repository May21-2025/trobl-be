package com.may21.trobl.storage;

import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class CdnCacheService {

    @Value("${GOOGLE_CLOUD_PROJECT_ID}")
    private String PROJECT_ID;

    @Value("${CDN_LB_DOMAIN}")
    private String urlMapName;

    private final GoogleCredentials credentials;
    private final HttpTransport httpTransport = new NetHttpTransport();
    private final JsonFactory jsonFactory = GsonFactory.getDefaultInstance();


    public void invalidateCdnCache(String fileName) {
        try {
            // 캐시 무효화할 경로 설정
            String objectPath = fileName;
            if (!objectPath.startsWith("/")) {
                objectPath = "/" + objectPath;
            }

            // CacheInvalidationRule 생성
            Map<String, Object> invalidationRule = new HashMap<>();
            invalidationRule.put("path", objectPath);

            // 요청 본문 생성
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("cacheInvalidationRules", List.of(invalidationRule));

            // API 호출
            GenericUrl url = new GenericUrl(String.format(
                    "https://compute.googleapis.com/compute/v1/projects/%s/global/urlMaps/%s/invalidateCache",
                    PROJECT_ID, urlMapName));

            HttpRequestFactory requestFactory =
                    httpTransport.createRequestFactory(new HttpCredentialsAdapter(credentials));

            HttpRequest request = requestFactory.buildPostRequest(url,
                    new JsonHttpContent(jsonFactory, requestBody));

            HttpResponse response = request.execute();

            if (response.isSuccessStatusCode()) {
                String responseContent = response.parseAsString();
                log.info("CDN cache invalidation successful for file: {}, Response: {}", fileName,
                        responseContent);
            }
            else {
                log.error("CDN cache invalidation failed for file: {}, Status: {}, Response: {}",
                        fileName, response.getStatusCode(), response.parseAsString());
            }

        } catch (Exception e) {
            log.error("Failed to invalidate CDN cache for file: {}", fileName, e);
            return;
        }
    }

    // 여러 파일 일괄 무효화
    public void invalidateCdnCacheMultiple(List<String> fileNames) {
        try {
            List<Map<String, Object>> rules = fileNames.stream()
                    .map(fileName -> {
                        String objectPath = fileName.startsWith("/") ? fileName : "/" + fileName;
                        Map<String, Object> rule = new HashMap<>();
                        rule.put("path", objectPath);
                        return rule;
                    })
                    .toList();

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("cacheInvalidationRules", rules);

            GenericUrl url = new GenericUrl(String.format(
                    "https://compute.googleapis.com/compute/v1/projects/%s/global/urlMaps/%s/invalidateCache",
                    PROJECT_ID, urlMapName));

            HttpRequestFactory requestFactory =
                    httpTransport.createRequestFactory(new HttpCredentialsAdapter(credentials));

            HttpRequest request = requestFactory.buildPostRequest(url,
                    new JsonHttpContent(jsonFactory, requestBody));

            HttpResponse response = request.execute();

            if (response.isSuccessStatusCode()) {
                log.info("CDN cache invalidation successful for {} files", fileNames.size());
            }
            else {
                log.error("CDN cache invalidation failed, Status: {}, Response: {}",
                        response.getStatusCode(), response.parseAsString());
                throw new RuntimeException("CDN cache invalidation failed");
            }

        } catch (Exception e) {
            log.error("Failed to invalidate CDN cache for multiple files", e);
            throw new RuntimeException("CDN cache invalidation failed", e);
        }
    }

    // 와일드카드 패턴으로 무효화
    public void invalidateCdnCacheByPattern(String pattern) {
        try {

            // 패턴이 /로 시작하지 않으면 추가
            String cachePath = pattern.startsWith("/") ? pattern : "/" + pattern;

            Map<String, Object> invalidationRule = new HashMap<>();
            invalidationRule.put("path", cachePath);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("cacheInvalidationRules", List.of(invalidationRule));

            GenericUrl url = new GenericUrl(String.format(
                    "https://compute.googleapis.com/compute/v1/projects/%s/global/urlMaps/%s/invalidateCache",
                    PROJECT_ID, urlMapName));

            HttpRequestFactory requestFactory =
                    httpTransport.createRequestFactory(new HttpCredentialsAdapter(credentials));

            HttpRequest request = requestFactory.buildPostRequest(url,
                    new JsonHttpContent(jsonFactory, requestBody));

            HttpResponse response = request.execute();

            if (response.isSuccessStatusCode()) {
                log.info("CDN cache invalidation successful for pattern: {}", pattern);
            }
            else {
                log.error("CDN cache invalidation failed for pattern: {}, Status: {}, Response: {}",
                        pattern, response.getStatusCode(), response.parseAsString());
                throw new RuntimeException("CDN cache invalidation failed");
            }

        } catch (Exception e) {
            log.error("Failed to invalidate CDN cache for pattern: {}", pattern, e);
            throw new RuntimeException("CDN cache invalidation failed", e);
        }
    }
}
