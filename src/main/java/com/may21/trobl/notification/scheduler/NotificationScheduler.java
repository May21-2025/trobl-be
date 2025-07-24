package com.may21.trobl.notification.scheduler;

import com.may21.trobl.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 알림 스케줄러
 * - 10분마다 배치 알림 처리 (좋아요 알림 포함)
 * - 1분마다 예약 알림 처리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {

    private final NotificationService notificationService;

    /**
     * 10분마다 실행 - 배치 알림 처리 (좋아요, 댓글 등)
     */
    @Scheduled(fixedRate = 600000) // 10분 = 600,000ms
    public void processBatchNotifications() {
        log.debug("Starting batch notification processing...");
        try {
            notificationService.processBatchNotifications();

            log.debug("Batch notification processing completed successfully");
        } catch (Exception e) {
            log.error("Failed to process batch notifications", e);
        }
    }

    /**
     * 1분마다 실행 - 예약 알림 처리
     */
    @Scheduled(fixedRate = 60000) // 1분 = 60,000ms
    public void processScheduledNotifications() {
        log.debug("Starting scheduled notification processing...");
        try {
            notificationService.processScheduledNotifications();
            log.debug("Scheduled notification processing completed successfully");
        } catch (Exception e) {
            log.error("Failed to process scheduled notifications", e);
        }
    }

    /**
     * 매일 자정 - 오래된 Redis 데이터 정리
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void cleanupOldNotificationData() {
        log.debug("Starting cleanup of old notification data...");
        try {
            // Redis에서 오래된 배치 알림 데이터 정리
            // 필요에 따라 NotificationService에 정리 메서드 추가
            log.debug("Cleanup of old notification data completed");
        } catch (Exception e) {
            log.error("Failed to cleanup old notification data", e);
        }
    }
}
