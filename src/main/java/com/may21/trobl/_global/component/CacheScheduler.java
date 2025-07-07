package com.may21.trobl._global.component;

import com.may21.trobl.post.service.PostingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CacheScheduler {
    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private PostingService postingService;

    // 매일 오후 12시에 캐시 갱신
    @Scheduled(cron = "0 0 12 * * ?")
    public void refreshTopPostsCache() {
        // 기존 캐시 삭제
        Cache cache = cacheManager.getCache("topPosts");
        if (cache != null) {
            cache.clear();
        }

        // 미리 캐시 워밍업
        String[] types = {"like", "view", "share", "comment", "vote", "default"};
        for (String type : types) {
            postingService.getTop10Views(type, null);
        }
    }
}
