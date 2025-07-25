package com.may21.trobl.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.may21.trobl._global.enums.ItemType;
import com.may21.trobl._global.enums.NotificationStrategy;
import com.may21.trobl._global.enums.NotificationType;
import com.may21.trobl._global.exception.BusinessException;
import com.may21.trobl._global.exception.ExceptionCode;
import com.may21.trobl.admin.AdminDto;
import com.may21.trobl.comment.dto.CommentDto;
import com.may21.trobl.notification.domain.ContentUpdateService;
import com.may21.trobl.notification.domain.Notification;
import com.may21.trobl.notification.domain.NotificationRepository;
import com.may21.trobl.notification.dto.NotificationDto;
import com.may21.trobl.post.domain.PostRepository;
import com.may21.trobl.post.domain.Posting;
import com.may21.trobl.post.dto.PostDto;
import com.may21.trobl.pushAlarm.PushNotificationService;
import com.may21.trobl.user.domain.User;
import com.may21.trobl.user.domain.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final NotificationRepository notificationRepository;
    private final PushNotificationService pushNotificationService;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ContentUpdateService contentUpdateService;
    private final NotificationBatchService notificationBatchService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String LIKE_TITLE = "많은 사람들이 당신의 이야기에 공감하고 있어요!";
    private static final String COMMENT_TITLE = "내 글에 누군가 댓글을 남겼어요! 어떤 이야기일까요?";
    private static final String FAIRVIEW_REQUEST_TITLE = "배우자가 페어뷰 작성을 요청하셨습니다.";
    private static final String FAIRVIEW_CONFIRMATION_TITLE = "배우자의 글 작성이 완료되었습니다.";
    private static final String PARTNER_REQUEST_TITLE = "${partnerName}님이 배우자 등록 신청이 왔습니다!";
    private static final String PARTNER_ACCEPTED_TITLE = "${partnerName}님이 배우자로 등록되었습니다.";
    private static final String PARTNER_DECLINED_TITLE = "${partnerName}님이 배우자 등록을 거절하셨습니다.";

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class NotificationBasicData {
        private ItemType itemType;
        private Long itemId;

        public String getItemType() {
            return itemType.name()
                    .toLowerCase();
        }

        public String getItemId() {
            return itemId.toString();
        }

        public NotificationBasicData(String itemType, String itemId) {
            this.itemType = ItemType.valueOf(itemType.toUpperCase());
            this.itemId = itemId.isEmpty() ? 0L : Long.parseLong(itemId);
        }
    }

    /**
     * 표준화된 데이터 맵 생성
     */
    private Map<String, String> createStandardDataMap(NotificationType type, String title,
            String body, NotificationBasicData itemData) {
        Map<String, String> data = new HashMap<>();
        data.put("type", type.name()
                .toLowerCase());
        data.put("title", title);
        data.put("body", body);
        data.put("itemType", itemData.getItemType());
        data.put("itemId", itemData.getItemId());
        return data;
    }

    /**
     * 알림 생성 및 전송
     */
    public void createAndSendNotification(Long userId, NotificationType type, String title,
            String body, NotificationBasicData data, NotificationStrategy strategy,
            LocalDateTime scheduledTime) {
        Map<String, String> dataMap = createStandardDataMap(type, title, body, data);
        Notification notification = new Notification(userId, type, dataMap, null);

        notificationRepository.save(notification);

        switch (strategy) {
            case IMMEDIATE:
                sendImmediateNotification(userId, title, body, dataMap);
                break;
            case BATCHED:
                queueForBatchNotification(userId, notification);
                break;
            case SCHEDULED:
                scheduleNotification(userId, notification, scheduledTime);
                break;
        }
    }

    private User getUserWithValidFcmTokens(Long userId) {
        User user = userRepository.findById(userId)
                .orElse(null);

        if (user == null || user.getFcmTokenList()
                .isEmpty()) {
            return null;
        }

        return user;
    }

    private void sendNotificationToUser(User user, String title, String body,
            Map<String, String> data, String logContext) {
        NotificationDto.SendRequest request = NotificationDto.SendRequest.builder()
                .userId(user.getId())
                .title(title)
                .body(body)
                .data(data)
                .build();

        try {
            pushNotificationService.sendNotificationTo(user.getFcmTokenList(), request);
            log.debug("{} notification sent to user: {}", logContext, user.getId());
        } catch (Exception e) {
            log.error("Failed to send {} notification to user: {}", logContext, user.getId(), e);
        }
    }

    /**
     * 즉시 전송 (비동기)
     */
    @Async("notificationTaskExecutor")
    @Override
    public void sendImmediateNotification(Long userId, String title, String body,
            Map<String, String> data) {
        User user = getUserWithValidFcmTokens(userId);
        if (user == null) {
            return;
        }
        sendNotificationToUser(user, title, body, data, "Immediate");
    }

    @Override
    public void queueForBatchNotification(Long userId, Notification notification) {
        String key = "batch_notifications:" + userId;
        redisTemplate.opsForList()
                .rightPush(key, notification.getId());
        redisTemplate.expire(key, Duration.ofMinutes(15));
        log.debug("Notification queued for batch processing: userId={}, notificationId={}", userId,
                notification.getId());
    }

    @Override
    public void scheduleNotification(Long userId, Notification notification,
            LocalDateTime scheduledTime) {
        if (scheduledTime == null) {
            scheduledTime = LocalDateTime.now()
                    .plusHours(1);
        }

        String key = "scheduled_notifications:" +
                scheduledTime.format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        redisTemplate.opsForList()
                .rightPush(key, notification.getId());

        Instant expireInstant = scheduledTime.plusHours(1)
                .atZone(ZoneId.systemDefault())
                .toInstant();
        redisTemplate.expireAt(key, expireInstant);

        log.debug("Notification scheduled: userId={}, notificationId={}, scheduledTime={}", userId,
                notification.getId(), scheduledTime);
    }

    @Override
    public void processBatchNotifications() {
        Set<String> userKeys = redisTemplate.keys("batch_notifications:*");

        if (userKeys != null) {
            for (String key : userKeys) {
                String userId = key.substring("batch_notifications:".length());
                List<Object> notificationIds = redisTemplate.opsForList()
                        .range(key, 0, -1);

                if (notificationIds != null && !notificationIds.isEmpty()) {
                    processBatchNotificationsForUser(Long.parseLong(userId), notificationIds);
                    redisTemplate.delete(key);
                }
            }
        }
    }

    private void processBatchNotificationsForUser(Long userId, List<Object> notificationIds) {
        List<Notification> notifications = notificationRepository.findAllById(
                notificationIds.stream()
                        .map(id -> Long.parseLong(id.toString()))
                        .toList());

        User user = userRepository.findById(userId)
                .orElse(null);
        if (Objects.isNull(user) || notifications.isEmpty()) return;

        Map<NotificationType, List<Notification>> groupedNotifications = notifications.stream()
                .collect(Collectors.groupingBy(Notification::getType));

        for (Map.Entry<NotificationType, List<Notification>> entry : groupedNotifications.entrySet()) {
            sendBatchNotification(user, entry.getKey(), entry.getValue());
        }
    }

    @Async("notificationTaskExecutor")
    public void sendBatchNotification(User user, NotificationType type,
            List<Notification> notifications) {
        String title = getBatchTitle(type);
        String body = getBatchBody(type, notifications.size());

        Map<String, String> data = new HashMap<>();
        data.put("type", type.name()
                .toLowerCase());
        data.put("title", title);
        data.put("body", body);
        data.put("count", String.valueOf(notifications.size()));

        sendNotificationToUser(user, title, body, data, "Batch");
    }

    private String getBatchTitle(NotificationType type) {
        return switch (type) {
            case LIKE -> LIKE_TITLE;
            case COMMENT -> COMMENT_TITLE;
            default -> "새로운 알림";
        };
    }

    private String getBatchBody(NotificationType type, int count) {
        return switch (type) {
            case LIKE -> String.format("%d개의 새로운 좋아요가 있습니다!", count);
            case COMMENT -> String.format("%d개의 새로운 댓글이 있습니다!", count);
            default -> String.format("%d개의 새로운 알림이 있습니다!", count);
        };
    }

    @Override
    public void processScheduledNotifications() {
        String currentTimeKey = "scheduled_notifications:" + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));

        List<Object> notificationIds = redisTemplate.opsForList()
                .range(currentTimeKey, 0, -1);

        if (notificationIds != null && !notificationIds.isEmpty()) {
            List<Notification> notifications = notificationRepository.findAllById(
                    notificationIds.stream()
                            .map(id -> Long.parseLong(id.toString()))
                            .collect(Collectors.toList()));

            for (Notification notification : notifications) {
                sendScheduledNotification(notification);
            }

            redisTemplate.delete(currentTimeKey);
        }
    }

    @Async("notificationTaskExecutor")
    public void sendScheduledNotification(Notification notification) {
        Map<String, String> data = fromJson(notification.getData());

        User user = getUserWithValidFcmTokens(notification.getUserId());
        if (user == null) {
            return;
        }
        String title = data.getOrDefault("title", "예약 알림");
        String body = data.getOrDefault("body", "예약된 알림입니다.");

        sendNotificationToUser(user, title, body, data, "Scheduled");
    }

    // 좋아요 알림 - 새로운 배치 서비스 사용
    @Override
    public void sendPostLikeNotification(Long postId, Long userId) {
        notificationBatchService.addPostLikeToQueue(postId, userId);
    }

    @Override
    public void sendCommentLikeNotification(Long commentId, Long userId) {
        notificationBatchService.addCommentLikeToQueue(commentId, userId);
    }

    @Override
    public void sendNewCommentNotification(Long postId, CommentDto.Response commentDto) {
        Posting post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.POST_NOT_FOUND));

        Long receiverUserId = post.getUserId();
        if (receiverUserId.equals(commentDto.getUserId())) {
            return;
        }

        String commentSnippet = getCommentSnippet(commentDto.getContent());
        NotificationBasicData itemData =
                new NotificationBasicData(ItemType.COMMENT, commentDto.getCommentId());
        createAndSendNotification(receiverUserId, NotificationType.COMMENT, COMMENT_TITLE,
                commentSnippet, itemData, NotificationStrategy.IMMEDIATE, null);
    }

    private String getCommentSnippet(String content) {
        return content.length() > 20 ? content.substring(0, 20) + "..." : content;
    }

    private Map<String, String> fromJson(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            return new HashMap<>();
        }
    }

    @Override
    public boolean markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.NOTIFICATION_NOT_FOUND));
        if (!notification.getUserId()
                .equals(userId)) {
            throw new BusinessException(ExceptionCode.FORBIDDEN);
        }
        notification.markAsRead();
        return true;
    }

    @Override
    public void setNotificationSetting(User user) {
        for (NotificationType type : NotificationType.values()) {
            user.setNotification(type, true);
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

    @Override
    public void notifyPostDeleted(Long targetUserId, PostDto.Notification info) {
        String title = "게시글이 삭제되었습니다";
        String body = String.format("게시글 '%s'이(가) 삭제되었습니다.", info.getTitle());

        NotificationBasicData itemData = new NotificationBasicData(ItemType.REPORT, 0L);
        createAndSendNotification(targetUserId, NotificationType.POST_DELETED, title, body,
                itemData, NotificationStrategy.IMMEDIATE, null);
    }

    @Override
    public boolean notifyMarketingAlert(AdminDto.PushNotification message) {
        User user = userRepository.findById(message.getUserId())
                .orElse(null);
        if (user == null || user.getFcmTokenList()
                .isEmpty()) return false;

        NotificationBasicData itemData = new NotificationBasicData(ItemType.REPORT, 0L);
        createAndSendNotification(user.getId(), NotificationType.MARKETING, message.getTitle(),
                message.getMessage(), itemData, NotificationStrategy.IMMEDIATE, null);
        return true;
    }

    @Override
    public void sendFairViewRequest(Long postId, User partner) {
        String body = "페어뷰 요청이 도착했습니다. 확인해주세요!";

        NotificationBasicData itemData = new NotificationBasicData(ItemType.POST, postId);
        createAndSendNotification(partner.getId(), NotificationType.FAIRVIEW_REQUEST,
                FAIRVIEW_REQUEST_TITLE, body, itemData, NotificationStrategy.IMMEDIATE, null);
    }

    @Override
    public void sendFairViewConfirmedRequest(Long fairViewId, Long userId) {
        Long postId = postRepository.findPostIdByFairViewId(fairViewId);
        if (postId == null) return;

        boolean confirmedByPartner = postRepository.isPostIdOwnerIsUser(postId, userId);
        if (confirmedByPartner) return;

        Long targetUserId = postRepository.findOwnerIdById(postId);
        contentUpdateService.fairViewConfirmUpdate(postId, targetUserId);

        String body = "페어뷰 게시가 가능합니다. 게시하겠습니까?";

        NotificationBasicData itemData = new NotificationBasicData(ItemType.POST, postId);
        createAndSendNotification(targetUserId, NotificationType.FAIRVIEW_REQUEST,
                FAIRVIEW_CONFIRMATION_TITLE, body, itemData, NotificationStrategy.IMMEDIATE, null);
    }

    @Override
    public void sendPartnerRequest(User targetUser, User sentUser) {
        String title = PARTNER_REQUEST_TITLE.replace("${partnerName}", sentUser.getNickname());
        String body = "배우자 등록 신청이 도착했습니다. 확인해주세요!";

        NotificationBasicData itemData = new NotificationBasicData(ItemType.USER, sentUser.getId());
        createAndSendNotification(targetUser.getId(), NotificationType.PARTNER_REQUEST, title, body,
                itemData, NotificationStrategy.IMMEDIATE, null);
    }

    @Override
    public void sendPartnerAccepted(User targetUser, User sentUser) {
        String title = PARTNER_ACCEPTED_TITLE.replace("${partnerName}", sentUser.getNickname());
        String body = "배우자 등록이 완료되었습니다!";

        NotificationBasicData itemData = new NotificationBasicData(ItemType.USER, sentUser.getId());
        createAndSendNotification(targetUser.getId(), NotificationType.PARTNER_ACCEPTED, title,
                body, itemData, NotificationStrategy.IMMEDIATE, null);
    }

    @Override
    public void sendPartnerDeclined(User targetUser, User sentUser) {
        String title = PARTNER_DECLINED_TITLE.replace("${partnerName}", sentUser.getNickname());
        String body = "배우자 등록 신청이 거절되었습니다. 다시 시도해주세요!";

        NotificationBasicData itemData = new NotificationBasicData(ItemType.USER, sentUser.getId());
        createAndSendNotification(targetUser.getId(), NotificationType.PARTNER_DECLINED, title,
                body, itemData, NotificationStrategy.IMMEDIATE, null);
    }

    @Override
    public boolean getMainNotification(Long userId) {
        return contentUpdateService.hasUserNewUpdates(userId);
    }

    @Override
    public NotificationDto.SubMenu getSubManuNotification(Long userId) {
        return contentUpdateService.getSubManuNotification(userId);
    }

    @Override
    public boolean readAllNotifications(Long userId) {
        List<Notification> notifications = notificationRepository.findByUserIdAndReadFalse(userId);
        if (notifications.isEmpty()) {
            return true;
        }
        for (Notification notification : notifications) {
            notification.markAsRead();
        }
        return false;
    }

    @Override
    public void testNotification(Long testUserId, NotificationType notificationType, String s,
            String s1, Map<String, String> itemType, NotificationStrategy notificationStrategy) {
        NotificationBasicData itemData =
                new NotificationBasicData(itemType.get("itemType"), itemType.get("itemId"));
        createAndSendNotification(testUserId, notificationType, s, s1, itemData,
                notificationStrategy, LocalDateTime.now()
                        .plusMinutes(1));
        log.info("Test notification sent to user: {}, type: {}, title: {}, body: {}, itemData: {}",
                testUserId, notificationType, s, s1, itemData);

    }
}
