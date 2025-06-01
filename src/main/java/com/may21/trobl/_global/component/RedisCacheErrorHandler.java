package com.may21.trobl._global.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.stereotype.Component;

@Component
public class RedisCacheErrorHandler implements CacheErrorHandler {

    private static final Logger logger = LoggerFactory.getLogger(RedisCacheErrorHandler.class);

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        logger.warn("Redis cache GET error for key: {}, cache: {}, error: {}",
                key, cache.getName(), exception.getMessage());
        // 캐시 조회 실패 시 null 반환하여 DB에서 조회하도록 함
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        logger.warn("Redis cache PUT error for key: {}, cache: {}, error: {}",
                key, cache.getName(), exception.getMessage());
        // 캐시 저장 실패 시 로그만 남기고 계속 진행
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        logger.warn("Redis cache EVICT error for key: {}, cache: {}, error: {}",
                key, cache.getName(), exception.getMessage());
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        logger.warn("Redis cache CLEAR error for cache: {}, error: {}",
                cache.getName(), exception.getMessage());
    }
}