package com.may21.trobl.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.may21.trobl._global.enums.NotificationStrategy;
import com.may21.trobl._global.enums.NotificationType;
import com.may21.trobl._global.exception.BusinessException;
import com.may21.trobl._global.exception.ExceptionCode;
import com.may21.trobl.comment.domain.Comment;
import com.may21.trobl.comment.domain.CommentRepository;
import com.may21.trobl.comment.dto.CommentDto;
import com.may21.trobl.notification.domain.Notification;
import com.may21.trobl.notification.domain.NotificationRepository;
import com.may21.trobl.notification.dto.NotificationDto;
import com.may21.trobl.post.domain.PostRepository;
import com.may21.trobl.post.domain.Posting;
import com.may21.trobl.user.domain.User;
import com.may21.trobl.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.may21.trobl._global.utility.Utility.toJson;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final NotificationRepository notificationRepository;
    private final PushNotificationService pushNotificationService;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;


    /**
     * 알림 생성 및 전송 전략에 따른 처리
     */
    public void createAndSendNotification(Long userId, NotificationType type, String title,
                                          String body, Map<String, String> data,
                                          NotificationStrategy strategy) {
        createAndSendNotification(userId, type, title, body, data, strategy, null);
    }

    /**
     * 예약 전송용 - 특정 시간 지정 가능
     */
    public void createAndSendNotification(Long userId, NotificationType type, String title,
                                          String body, Map<String, String> data,
                                          NotificationStrategy strategy, LocalDateTime scheduledTime) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .body(body)
                .data(toJson(data))
                .scheduledTime(scheduledTime) // 예약 시간 추가
                .build();

        notificationRepository.save(notification);

        switch (strategy) {
            case IMMEDIATE:
                sendImmediateNotification(userId, title, body, data);
                break;
            case BATCHED:
                queueForBatchNotification(userId, notification);
                break;
            case SCHEDULED:
                scheduleNotification(userId, notification, scheduledTime);
                break;
        }
    }

    /**
     * 1. 즉시 전송
     */
    @Override
    public void sendImmediateNotification(Long userId, String title, String body, Map<String, String> data) {
        NotificationDto.SendRequest request = NotificationDto.SendRequest.builder()
                .userId(userId)
                .title(title)
                .body(body)
                .data(data)
                .build();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));
        try {
            pushNotificationService.sendNotificationTo(request);
            log.info("Immediate notification sent to user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to send immediate notification to user: {}", userId, e);
        }
    }

    /**
     * 2. 10분마다 일괄 전송용 큐잉
     */
    @Override
    public void queueForBatchNotification(Long userId, Notification notification) {
        String key = "batch_notifications:" + userId;
        redisTemplate.opsForList().rightPush(key, notification.getId());
        redisTemplate.expire(key, Duration.ofMinutes(15)); // 여유시간 포함
        log.debug("Notification queued for batch processing: userId={}, notificationId={}",
                userId, notification.getId());
    }

    /**
     * 3. 예약 전송용 스케줄링
     */
    @Override
    public void scheduleNotification(Long userId, Notification notification, LocalDateTime scheduledTime) {
        if (scheduledTime == null) {
            scheduledTime = LocalDateTime.now().plusHours(1); // 기본 1시간 후
        }

        String key = "scheduled_notifications:" + scheduledTime.format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        redisTemplate.opsForList().rightPush(key, notification.getId());
        redisTemplate.expireAt(key, Instant.from(scheduledTime.plusHours(1))); // 스케줄 시간 + 1시간 후 만료

        log.info("Notification scheduled: userId={}, notificationId={}, scheduledTime={}",
                userId, notification.getId(), scheduledTime);
    }

    /**
     * 일괄 처리용 - 스케줄러에서 10분마다 호출
     */
    @Override
    public void processBatchNotifications() {
        Set<String> userKeys = redisTemplate.keys("batch_notifications:*");

        for (String key : userKeys) {
            String userId = key.substring("batch_notifications:".length());
            List<Object> notificationIds = redisTemplate.opsForList().range(key, 0, -1);

            if (!Objects.requireNonNull(notificationIds).isEmpty()) {
                processBatchNotificationsForUser(Long.parseLong(userId), notificationIds);
                redisTemplate.delete(key); // 처리 완료 후 키 삭제
            }
        }
    }

    private void processBatchNotificationsForUser(Long userId, List<Object> notificationIds) {
        List<Notification> notifications = notificationRepository.findAllById(
                notificationIds.stream()
                        .map(id -> Long.parseLong(id.toString()))
                        .toList()
        );

        if (notifications.isEmpty()) return;

        // 같은 타입별로 그룹핑하여 요약
        Map<NotificationType, List<Notification>> groupedNotifications =
                notifications.stream().collect(Collectors.groupingBy(Notification::getType));

        for (Map.Entry<NotificationType, List<Notification>> entry : groupedNotifications.entrySet()) {
            sendBatchNotification(userId, entry.getKey(), entry.getValue());
        }
    }

    private void sendBatchNotification(Long userId, NotificationType type, List<Notification> notifications) {
        String title, body;
        Map<String, String> data = new HashMap<>();

        switch (type) {
            case LIKE:
                title = "새로운 좋아요 알림";
                body = String.format("%d개의 새로운 좋아요가 있습니다!", notifications.size());
                break;
            case COMMENT:
                title = "새로운 댓글 알림";
                body = String.format("%d개의 새로운 댓글이 있습니다!", notifications.size());
                break;
            default:
                title = "새로운 알림";
                body = String.format("%d개의 새로운 알림이 있습니다!", notifications.size());
        }

        data.put("count", String.valueOf(notifications.size()));
        data.put("type", type.name());

        NotificationDto.SendRequest request = NotificationDto.SendRequest.builder()
                .userId(userId)
                .title(title)
                .body(body)
                .data(data)
                .build();

        try {
            pushNotificationService.sendNotificationTo(request);
            log.info("Batch notification sent to user: {}, type: {}, count: {}",
                    userId, type, notifications.size());
        } catch (Exception e) {
            log.error("Failed to send batch notification to user: {}", userId, e);
        }
    }

    /**
     * 예약 알림 처리용 - 스케줄러에서 1분마다 호출
     */
    @Override
    public void processScheduledNotifications() {
        String currentTimeKey = "scheduled_notifications:" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));

        List<Object> notificationIds = redisTemplate.opsForList().range(currentTimeKey, 0, -1);

        if (!notificationIds.isEmpty()) {
            List<Notification> notifications = notificationRepository.findAllById(
                    notificationIds.stream()
                            .map(id -> Long.parseLong(id.toString()))
                            .collect(Collectors.toList())
            );

            for (Notification notification : notifications) {
                sendScheduledNotification(notification);
            }

            redisTemplate.delete(currentTimeKey);
        }
    }

    private void sendScheduledNotification(Notification notification) {
        Map<String, String> data = fromJson(notification.getData());

        NotificationDto.SendRequest request = NotificationDto.SendRequest.builder()
                .userId(notification.getUserId())
                .title(notification.getTitle())
                .body(notification.getBody())
                .data(data)
                .build();

        try {
            pushNotificationService.sendNotificationTo(request);
            log.info("Scheduled notification sent: notificationId={}, userId={}",
                    notification.getId(), notification.getUserId());
        } catch (Exception e) {
            log.error("Failed to send scheduled notification: {}", notification.getId(), e);
        }
    }

    // 기존 메서드들을 새로운 전략 방식으로 수정
    @Override
    public void sendPostLikeNotification(Long postId, Long userId) {
        Posting post = postRepository.findById(postId).orElse(null);
        if (post == null || post.getUserId().equals(userId)) {
            return;
        }

        // 좋아요는 일괄 처리로 (너무 많이 올 수 있으므로)
        createAndSendNotification(
                post.getUserId(),
                NotificationType.LIKE,
                "새로운 좋아요 ✨",
                "당신의 게시글이 좋아요를 받았습니다!",
                Map.of("postId", postId.toString()),
                NotificationStrategy.BATCHED
        );
    }

    @Override
    public void sendCommentLikeNotification(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment == null || comment.getUserId().equals(userId)) {
            return;
        }

        // 댓글 좋아요도 일괄 처리
        createAndSendNotification(
                comment.getUserId(),
                NotificationType.LIKE,
                "새로운 좋아요 ✨",
                "당신의 댓글이 좋아요를 받았습니다!",
                Map.of("commentId", commentId.toString()),
                NotificationStrategy.BATCHED
        );
    }

    @Override
    public void sendNewCommentNotification(Long postId, CommentDto.Response commentDto) {
        Posting post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.POST_NOT_FOUND));

        Long receiverUserId = post.getUserId();
        if (receiverUserId.equals(commentDto.getUserId())) {
            return;
        }

        String commentSnippet = commentDto.getContent();
        if (commentSnippet.length() > 20) {
            commentSnippet = commentSnippet.substring(0, 20) + "...";
            createAndSendNotification(
                    receiverUserId,
                    NotificationType.COMMENT,
                    "새 댓글이 달렸어요! ✨",
                    commentSnippet,
                    Map.of(
                            "postId", postId.toString(),
                            "commentId", commentDto.getCommentId().toString()
                    ),
                    NotificationStrategy.IMMEDIATE
            );
        } else {
            createAndSendNotification(
                    receiverUserId,
                    NotificationType.COMMENT,
                    "새 댓글이 달렸어요! ✨",
                    commentSnippet,
                    Map.of(
                            "postId", postId.toString(),
                            "commentId", commentDto.getCommentId().toString()
                    ),
                    NotificationStrategy.IMMEDIATE
            );
        }

        // 새 댓글은 즉시 알림 (중요한 상호작용)
    }

    // 관리자용 공지사항 등 예약 전송 예시
    public void sendScheduledAnnouncement(List<Long> userIds, String title, String body,
                                          LocalDateTime scheduledTime) {
        for (Long userId : userIds) {
            createAndSendNotification(
                    userId,
                    NotificationType.ANNOUNCEMENT,
                    title,
                    body,
                    Map.of("type", "announcement"),
                    NotificationStrategy.SCHEDULED,
                    scheduledTime
            );
        }
    }

    private Map fromJson(String json) {
        try {
            return new ObjectMapper().readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            return new HashMap<>();
        }
    }


    public void createAndSendNotification(Long userId, NotificationType type, String title, String body, Map<String, String> data) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .body(body)
                .data(toJson(data))
                .build();
        notificationRepository.save(notification);

        NotificationDto.SendRequest request = NotificationDto.SendRequest.builder()
                .userId(userId)
                .title(title)
                .body(body)
                .data(data)
                .build();

        try {
            pushNotificationService.sendNotificationTo(request);
        } catch (Exception e) {
            log.error("Failed to send push notification", e);
            // 실패해도 알림 DB 저장은 했으니 무시할 수도 있음
        }
    }


    @Override
    public boolean markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.NOTIFICATION_NOT_FOUND));
        if (!notification.getUserId()
                .equals(userId)) throw new BusinessException(ExceptionCode.FORBIDDEN)
                ;
        notification.markAsRead();
        return true;
    }

    @Override
    public void setNotificationSetting(User user) {
        // 알림 설정을 초기화
        for (NotificationType type : NotificationType.values()) {
            user.setNotification(type, true); // 기본적으로 모든 알림을 활성화
        }
        userRepository.save(user);
    }

    @Override
    public boolean setNotificationSettings(Long userId, String notificationType, Boolean enabled) {
        NotificationType type = NotificationType.valueOf(notificationType);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));
        user.setNotification(type, enabled);
        return false;
    }

    public void queueNotification(Long userId, Notification notification) {
        notificationRepository.save(notification);
        String key = "pending_notifications:" + userId;
        redisTemplate.opsForList().rightPush(key, notification.getId());
        redisTemplate.expire(key, Duration.ofMinutes(10));
    }
}