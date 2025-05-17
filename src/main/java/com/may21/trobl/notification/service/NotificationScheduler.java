package com.may21.trobl.notification.service;

import com.may21.trobl._global.enums.NotificationType;
import com.may21.trobl.notification.domain.Notification;
import com.may21.trobl.notification.domain.NotificationRepository;
import com.may21.trobl.notification.dto.NotificationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {

    private final RedisTemplate<String, Object> redisTemplate;
    private final NotificationRepository notificationRepository;
    private final PushNotificationService pushNotificationService;

    @Scheduled(fixedDelay = 30000) // 30초마다 실행
    public void flushPendingNotifications() {
        Set<String> keys = redisTemplate.keys("pending_notifications:*");

        if (keys == null || keys.isEmpty()) {
            return;
        }

        for (String key : keys) {
            Long userId = extractUserIdFromKey(key);

            List<Object> notificationIds = redisTemplate.opsForList().range(key, 0, -1);
            if (notificationIds == null || notificationIds.isEmpty()) {
                continue;
            }

            List<Long> ids = notificationIds.stream()
                    .map(obj -> Long.parseLong(obj.toString()))
                    .toList();

            List<Notification> notifications = notificationRepository.findAllById(ids);

            // 요약 메시지 만들기
            String message = createSummaryMessage(notifications);

            // 푸시 발송
            try {
                pushNotificationService.sendNotificationTo(NotificationDto.SendRequest.builder()
                        .userId(userId)
                        .title("새로운 활동 알림 ✨")
                        .body(message)
                        .data(Map.of("type", "summary"))
                        .build());
            } catch (Exception e) {
                log.error("푸시 발송 실패 userId={}", userId, e);
            }

            // 푸시가 끝났으면 대기 큐 삭제
            redisTemplate.delete(key);
        }
    }

    private Long extractUserIdFromKey(String key) {
        return Long.parseLong(key.replace("pending_notifications:", ""));
    }

    private String createSummaryMessage(List<Notification> notifications) {
        long commentCount = notifications.stream().filter(n -> n.getType() == NotificationType.COMMENT).count();
        long likeCount = notifications.stream().filter(n -> n.getType() == NotificationType.LIKE).count();
        long voteCount = notifications.stream().filter(n -> n.getType() == NotificationType.VOTE).count();

        List<String> parts = new ArrayList<>();
        if (commentCount > 0) parts.add(commentCount + "개의 댓글");
        if (likeCount > 0) parts.add(likeCount + "개의 좋아요");
        if (voteCount > 0) parts.add(voteCount + "개의 투표 참여");

        if (parts.isEmpty()) return "새로운 활동이 있습니다!";
        return String.join(", ", parts) + "가 발생했습니다.";
    }
}
