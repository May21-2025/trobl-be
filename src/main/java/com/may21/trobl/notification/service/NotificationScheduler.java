package com.may21.trobl.notification.service;

import com.may21.trobl._global.enums.NotificationStrategy;
import com.may21.trobl._global.enums.NotificationType;
import com.may21.trobl._global.exception.BusinessException;
import com.may21.trobl._global.exception.ExceptionCode;
import com.may21.trobl.notification.domain.Notification;
import com.may21.trobl.notification.domain.NotificationRepository;
import com.may21.trobl.notification.domain.NotificationSetting;
import com.may21.trobl.notification.dto.NotificationDto;
import com.may21.trobl.user.domain.User;
import com.may21.trobl.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static com.may21.trobl._global.utility.Utility.toJson;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {

    private final NotificationService notificationService;
    private final NotificationMessageService messageService;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;


    public void sendNotification(Long userId, NotificationType type, Map<String, Object> params) {
        sendNotification(userId, type, params, type.getDefaultStrategy(), null);
    }

    /**
     * 전략과 스케줄 시간을 직접 지정하는 메서드
     */
    public void sendNotification(Long userId, NotificationType type, Map<String, Object> params,
                                 NotificationStrategy strategy, LocalDateTime scheduledTime) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));
        NotificationSetting setting = user.getSetting();
        // 사용자 알림 설정 확인
        if (setting == null) {
            log.debug("Notification disabled for user: {}, type: {}", userId, type);
            return;
        }
        Locale userLocale = getUserLocale(user);
        NotificationDto.Message message = messageService.getMessage(type, userLocale, params);

        // 데이터 맵 구성
        Map<String, String> data = buildNotificationData(type, params);

        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .title(message.getTitle())
                .body(message.getBody())
                .data(toJson(data))
                .scheduledTime(scheduledTime)
                .build();

        notificationRepository.save(notification);

        switch (strategy) {
            case IMMEDIATE:
                notificationService.sendImmediateNotification(userId, message.getTitle(), message.getBody(), data);
                break;
            case BATCHED:
                notificationService.queueForBatchNotification(userId, notification);
                break;
            case SCHEDULED:
                notificationService.scheduleNotification(userId, notification, scheduledTime);
                break;
        }
    }

    /**
     * 간편 메서드들 - 각 타입별 특화된 파라미터
     */
    public void sendCommentNotification(Long userId, String commenterName, String contentSnippet, Long postId, Long commentId) {
        Map<String, Object> params = Map.of(
                "commenterName", commenterName,
                "contentSnippet", contentSnippet,
                "postId", postId.toString(),
                "commentId", commentId.toString()
        );
        sendNotification(userId, NotificationType.COMMENT, params);
    }

    public void sendLikeNotification(Long userId, String targetType, Long targetId) {
        Map<String, Object> params = Map.of(
                "targetType", targetType,
                "targetId", targetId.toString()
        );
        sendNotification(userId, NotificationType.LIKE, params);
    }

    public void sendContentRecommendationNotification(Long userId, String postTitle, Long postId) {
        Map<String, Object> params = Map.of(
                "postTitle", postTitle,
                "postId", postId.toString()
        );
        // 매일 9시 스케줄링
        LocalDateTime scheduledTime = LocalDateTime.now().withHour(9).withMinute(0).withSecond(0);
        if (scheduledTime.isBefore(LocalDateTime.now())) {
            scheduledTime = scheduledTime.plusDays(1);
        }
        sendNotification(userId, NotificationType.CONTENT_RECOMMENDATION, params,
                NotificationStrategy.SCHEDULED, scheduledTime);
    }

    public void sendFairviewRequestNotification(Long userId, Long requesterId) {
        Map<String, Object> params = Map.of(
                "requesterId", requesterId.toString()
        );
        sendNotification(userId, NotificationType.FAIRVIEW_REQUEST, params,
                NotificationStrategy.IMMEDIATE, null);
    }

    public void sendCommunityAnalysisNotification(Long userId) {
        Map<String, Object> params = Map.of(
                "analysisMonth", LocalDate.now().minusMonths(1).getMonth().toString()
        );
        sendNotification(userId, NotificationType.COMMUNITY_ANALYSIS, params);
    }

    public void sendAnnouncementNotification(List<Long> userIds, String title, String body, LocalDateTime scheduledTime) {
        Map<String, Object> params = Map.of(
                "announcementTitle", title,
                "announcementBody", body
        );

        for (Long userId : userIds) {
            sendNotification(userId, NotificationType.ANNOUNCEMENT, params,
                    NotificationStrategy.SCHEDULED, scheduledTime);
        }
    }

    /**
     * 일괄 처리 시 메시지 생성
     */
    private void sendBatchNotification(Long userId, NotificationType type, List<Notification> notifications) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));

        Locale userLocale = getUserLocale(user);
        Map<String, Object> params = Map.of("batchCount", notifications.size());
        NotificationDto.Message message = messageService.getMessage(type, userLocale, params);

        Map<String, String> data = Map.of(
                "type", type.name(),
                "count", String.valueOf(notifications.size()),
                "notificationIds", notifications.stream()
                        .map(n -> n.getId().toString())
                        .collect(Collectors.joining(","))
        );

        notificationService.sendImmediateNotification(userId, message.getTitle(), message.getBody(), data);
    }

    private Map<String, String> buildNotificationData(NotificationType type, Map<String, Object> params) {
        Map<String, String> data = new HashMap<>();
        data.put("type", type.name());

        // 타입별 필요한 데이터만 추가
        params.forEach((key, value) -> {
            if (value != null) {
                data.put(key, value.toString());
            }
        });

        return data;
    }

    private Locale getUserLocale(User user) {
        // 사용자 언어 설정 조회 (기본값: 한국어)
        String language = user.getLanguage() != null ? user.getLanguage().getCode() : "ko";
        return new Locale(language);
    }
//    /**
//     * 매일 오전 9시 - 콘텐츠 추천 및 인기글 알림
//     */
//    @Scheduled(cron = "0 0 9 * * *")
//    public void sendDailyRecommendations() {
//        log.info("Starting daily content recommendations at 9 AM");
//        // 추천 알고리즘에 따른 콘텐츠 추천 로직
//        // notificationService.sendContentRecommendationNotification(...);
//    }
//
//    /**
//     * 매월 1일 오전 10시 - 커뮤니티 분석 알림
//     */
//    @Scheduled(cron = "0 0 10 1 * *")
//    public void sendMonthlyAnalysis() {
//        log.info("Starting monthly community analysis notifications");
//        // 모든 활성 사용자에게 커뮤니티 분석 알림
//        // notificationService.sendCommunityAnalysisNotification(...);
//    }
//
//    /**
//     * 10분마다 - 일괄 알림 처리
//     */
//    @Scheduled(fixedRate = 600000)
//    public void processBatchNotifications() {
//        notificationService.processBatchNotifications();
//    }
//
//    /**
//     * 1분마다 - 예약 알림 처리
//     */
//    @Scheduled(fixedRate = 60000)
//    public void processScheduledNotifications() {
//        notificationService.processScheduledNotifications();
//    }
}

