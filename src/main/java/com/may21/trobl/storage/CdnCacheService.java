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

    private String urlMapName = "trobl-cdn";

    private final GoogleCredentials credentials;
    private final HttpTransport httpTransport = new NetHttpTransport();
    private final JsonFactory jsonFactory = GsonFactory.getDefaultInstance();


    public void invalidateCdnCache(String fileName) {
        try {
            String normalizedPath = normalizePath(fileName);

            log.info("🔄 Starting CDN cache invalidation for: {}", normalizedPath);

            // 올바른 Google Cloud CDN API 요청 형식
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("path", normalizedPath);

            log.info("📤 Request body: {}", requestBody);

            executeInvalidationRequest(requestBody, normalizedPath);

        } catch (Exception e) {
            log.error("❌ Failed to invalidate CDN cache for: {}", fileName, e);
        }
    }
    private void executeInvalidationRequest(Map<String, Object> requestBody, String path) {
        try {
            // API 엔드포인트 URL
            String apiUrl = String.format(
                    "https://compute.googleapis.com/compute/v1/projects/%s/global/urlMaps/%s/invalidateCache",
                    PROJECT_ID, urlMapName);

            GenericUrl url = new GenericUrl(apiUrl);

            // HTTP 요청 생성
            HttpRequestFactory requestFactory = httpTransport.createRequestFactory(
                    new HttpCredentialsAdapter(credentials));

            // JSON 본문 생성
            JsonHttpContent jsonContent = new JsonHttpContent(jsonFactory, requestBody);
            HttpRequest request = requestFactory.buildPostRequest(url, jsonContent);

            // 요청 헤더 설정
            request.getHeaders().setContentType("application/json");
            request.getHeaders().set("User-Agent", "trobl-backend/1.0");
            HttpResponse response = request.execute();

            // 응답 처리
            String responseContent = response.parseAsString();

            if (response.isSuccessStatusCode()) {
                log.info("✅ CDN cache invalidation successful for: {}", path);
                log.info("📥 Response: {}", responseContent);
            } else {
                log.error("❌ CDN cache invalidation failed for: {}", path);
                log.error("📥 Status: {}, Response: {}", response.getStatusCode(), responseContent);
            }

        } catch (HttpResponseException e) {
            log.error("❌ HTTP error for path: {}", path);
            log.error("   Status: {}", e.getStatusCode());
            log.error("   Message: {}", e.getStatusMessage());
            log.error("   Content: {}", e.getContent());

            // 400 에러 분석
            if (e.getStatusCode() == 400) {
                log.error("🔍 Bad Request Analysis:");
                log.error("   - Check if path '{}' is valid", path);
                log.error("   - Path should start with '/' and be URL-encoded if needed");
                log.error("   - For all content use '/*'");
            }

        } catch (Exception e) {
            log.error("❌ Unexpected error for path: {}", path, e);
        }
    }

    /**
     * 경로 정규화 및 검증
     */
    private String normalizePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            log.warn("⚠️ Empty path provided, using wildcard '/*'");
            return "/*";
        }

        String normalized = path.trim();

        // 앞에 슬래시가 없으면 추가
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }

        // 중복 슬래시 제거
        normalized = normalized.replaceAll("/+", "/");

        // URL 인코딩 (필요한 경우)
        // normalized = URLEncoder.encode(normalized, StandardCharsets.UTF_8).replace("%2F", "/");

        log.debug("🔧 Path normalized: '{}' → '{}'", path, normalized);

        return normalized;
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
