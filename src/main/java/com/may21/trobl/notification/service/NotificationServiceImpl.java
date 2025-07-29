package com.may21.trobl.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.may21.trobl._global.enums.ItemType;
import com.may21.trobl._global.enums.NotificationStrategy;
import com.may21.trobl._global.enums.NotificationType;
import com.may21.trobl._global.exception.BusinessException;
import com.may21.trobl._global.exception.ExceptionCode;
import com.may21.trobl.admin.AdminDto;
import com.may21.trobl.comment.domain.Comment;
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
    private static final String PARTNER_REQUEST_TITLE = "${partnerName}님이\n배우자 등록 신청을 하였습니다.";
    private static final String PARTNER_ACCEPTED_TITLE = "${partnerName}님이\n배우자로 등록되었습니다.";
    private static final String PARTNER_DECLINED_TITLE = "${partnerName}님이\n배우자 등록을 거절하셨습니다.";

    public void createAndSendNotification(Long userId, NotificationDto.SendRequest request,
            NotificationStrategy strategy, LocalDateTime scheduledTime) {

        switch (strategy) {
            case IMMEDIATE:
                sendImmediateNotification(userId, request);
                break;
            case BATCHED:
                queueForBatchNotification(userId, request);
                break;
            case SCHEDULED:
                scheduleNotification(userId, request, scheduledTime);
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

    /**
     * 즉시 전송 (비동기)
     */
    @Async("notificationTaskExecutor")
    @Override
    public void sendImmediateNotification(Long userId, NotificationDto.SendRequest request) {
        User user = getUserWithValidFcmTokens(userId);
        if (user == null) {
            return;
        }
        try {
            NotificationType type = request.getNotificationType();
            pushNotificationService.sendNotificationTo(user.getFcmTokenList(), request);
            log.error("{} notification sent to user: {}", type, user.getId());
        } catch (Exception e) {
            log.error("Failed to send {} notification to user: {}", "Immediate", user.getId(), e);
        }
    }

    @Override
    public void queueForBatchNotification(Long userId, NotificationDto.SendRequest request) {
        log.debug("{} notification sent to user: {}", request.getNotificationType(), userId);
        Long notificationId = notificationRepository.save(new Notification(userId, request, null))
                .getId();
        String key = "batch_notifications:" + notificationId;
        redisTemplate.opsForList()
                .rightPush(key, notificationId);
        redisTemplate.expire(key, Duration.ofMinutes(15));
        log.debug("Notification queued for batch processing: userId={}, notificationId={}", userId,
                notificationId);
    }

    @Override
    public void scheduleNotification(Long userId, NotificationDto.SendRequest request,
            LocalDateTime scheduledTime) {
        if (scheduledTime == null) {
            scheduledTime = LocalDateTime.now()
                    .plusHours(1);
        }

        Long notificationId =
                notificationRepository.save(new Notification(userId, request, scheduledTime))
                        .getId();

        String key = "scheduled_notifications:" +
                scheduledTime.format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        redisTemplate.opsForList()
                .rightPush(key, notificationId);

        Instant expireInstant = scheduledTime.plusHours(1)
                .atZone(ZoneId.systemDefault())
                .toInstant();
        redisTemplate.expireAt(key, expireInstant);

        log.debug("Notification scheduled: userId={}, notificationId={}, scheduledTime={}", userId,
                notificationId, scheduledTime);
    }

    @Override
    public void processBatchNotifications() {
        Set<String> userKeys = redisTemplate.keys("batch_notifications:*");
        log.debug("{} batch notifications processed", userKeys.size());
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
        data.put("count", String.valueOf(notifications.size()));

        NotificationDto.SendRequest request = NotificationDto.SendRequest.builder()
                .userId(user.getId())
                .title(title)
                .body(body)
                .itemType(ItemType.POST)
                .itemId(0L)
                .notificationType(type)
                .data(data)
                .build();

        try {
            pushNotificationService.sendNotificationTo(user.getFcmTokenList(), request);
            log.debug("{} notification sent to user: {}", "batch", user.getId());
        } catch (Exception e) {
            log.error("Failed to send {} notification to user: {}", "batch", user.getId(), e);
        }
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
        User user = getUserWithValidFcmTokens(notification.getUserId());
        if (user == null) {
            return;
        }
        String title = notification.getTitle();
        String body = notification.getBody();

        pushNotificationService.sendNotificationTo(user.getFcmTokenList(),
                NotificationDto.SendRequest.builder()
                        .userId(notification.getUserId())
                        .title(title)
                        .body(body)
                        .itemType(notification.getItemType())
                        .itemId(notification.getItemId())
                        .notificationType(notification.getType())
                        .build());
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
    public void sendNewLikeNotification(Long itemId, ItemType itemType) {
        if (itemType == ItemType.COMMENT) return;
        Long userId = postRepository.findOwnerIdById(itemId);
        User receiver = userRepository.findById(userId)
                .orElse(null);
        if (receiver == null || receiver.isNotificationBlocked(NotificationType.LIKE)) return;
        NotificationDto.SendRequest request = NotificationDto.SendRequest.builder()
                .userId(userId)
                .title(LIKE_TITLE)
                .body("")
                .itemType(itemType)
                .itemId(itemId)
                .notificationType(NotificationType.LIKE)
                .build();
        createAndSendNotification(userId, request, NotificationStrategy.IMMEDIATE, null);
    }


    @Override
    public void sendNewCommentNotification(Long postId, CommentDto.Response commentDto) {
        Posting post = postRepository.findById(postId)
                .orElse(null);
        if (post == null) return;
        Long receiverUserId = post.getUserId();
        User receiver = userRepository.findById(receiverUserId)
                .orElse(null);
        if (receiver == null || receiverUserId.equals(commentDto.getUserId()) ||
                receiver.isNotificationBlocked(NotificationType.COMMENT)) {
            return;
        }
        String commentSnippet = getCommentSnippet(commentDto.getContent());
        NotificationDto.SendRequest request = NotificationDto.SendRequest.builder()
                .userId(receiverUserId)
                .title(COMMENT_TITLE)
                .body(commentSnippet)
                .itemType(ItemType.POST)
                .itemId(postId)
                .notificationType(NotificationType.COMMENT)
                .build();
        createAndSendNotification(receiverUserId, request, NotificationStrategy.IMMEDIATE, null);
    }

    private String getCommentSnippet(String content) {
        return content.length() > 20 ? content.substring(0, 20) + "..." : content;
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

        NotificationDto.SendRequest request = NotificationDto.SendRequest.builder()
                .userId(targetUserId)
                .title(title)
                .body(body)
                .itemType(ItemType.POST)
                .itemId(info.getPostId())
                .notificationType(NotificationType.POST_DELETED)
                .build();

        createAndSendNotification(targetUserId, request, NotificationStrategy.IMMEDIATE, null);
    }

    @Override
    public boolean notifyMarketingAlert(AdminDto.PushNotification message) {
        User user = userRepository.findById(message.getUserId())
                .orElse(null);
        if (user == null || user.getFcmTokenList()
                .isEmpty() || user.isNotificationBlocked(NotificationType.MARKETING)) return false;

        NotificationDto.SendRequest request = NotificationDto.SendRequest.builder()
                .userId(user.getId())
                .title(message.getTitle())
                .body(message.getMessage())
                .itemType(ItemType.POST)
                .itemId(0L) // 마케팅 알림은 특정 아이템이 없으므로 0으로 설정
                .notificationType(NotificationType.MARKETING)
                .data(message.getData())
                .build();
        createAndSendNotification(user.getId(), request, NotificationStrategy.IMMEDIATE, null);
        return true;
    }

    @Override
    public void sendFairViewRequest(Long postId, User partner) {
        String body = "페어뷰 요청이 도착했습니다. 확인해주세요!";

        NotificationDto.SendRequest request = NotificationDto.SendRequest.builder()
                .userId(partner.getId())
                .title(FAIRVIEW_REQUEST_TITLE)
                .body(body)
                .itemType(ItemType.POST)
                .itemId(postId)
                .notificationType(NotificationType.FAIRVIEW_REQUEST)
                .build();
        createAndSendNotification(partner.getId(), request, NotificationStrategy.IMMEDIATE, null);
    }

    @Override
    public void sendFairViewConfirmedRequest(Long fairViewId) {
        Long postId = postRepository.findPostIdByFairViewId(fairViewId);
        if (postId == null) return;

        Long targetUserId = postRepository.findOwnerIdById(postId);
        contentUpdateService.fairViewConfirmUpdate(postId, targetUserId);

        String body = "페어뷰 게시가 가능합니다. 게시하겠습니까?";

        NotificationDto.SendRequest request = NotificationDto.SendRequest.builder()
                .userId(targetUserId)
                .title(FAIRVIEW_CONFIRMATION_TITLE)
                .body(body)
                .itemType(ItemType.POST)
                .itemId(postId)
                .notificationType(NotificationType.FAIRVIEW_CONFIRMATION)
                .build();
        createAndSendNotification(targetUserId, request, NotificationStrategy.IMMEDIATE, null);
    }

    @Override
    public void sendPartnerRequest(User targetUser, User sentUser) {
        String title = PARTNER_REQUEST_TITLE.replace("${partnerName}", sentUser.getNickname());
        String body = "배우자 등록 신청이 도착했습니다. 확인해주세요!";
        NotificationDto.SendRequest request = NotificationDto.SendRequest.builder()
                .userId(targetUser.getId())
                .title(title)
                .body(body)
                .itemType(ItemType.USER)
                .itemId(sentUser.getId())
                .notificationType(NotificationType.PARTNER_REQUEST)
                .build();
        createAndSendNotification(targetUser.getId(), request, NotificationStrategy.IMMEDIATE,
                null);
    }

    @Override
    public void sendPartnerAccepted(User targetUser, User sentUser) {
        String title = PARTNER_ACCEPTED_TITLE.replace("${partnerName}", sentUser.getNickname());
        String body = "배우자 등록이 완료되었습니다!";
        List<NotificationDto.SendRequest> requests = new ArrayList<>();
        requests.add(NotificationDto.SendRequest.builder()
                .userId(targetUser.getId())
                .title(title)
                .body(body)
                .itemType(ItemType.USER)
                .itemId(sentUser.getId())
                .notificationType(NotificationType.PARTNER_ACCEPTED)
                .build());
        requests.add(NotificationDto.SendRequest.builder()
                .userId(sentUser.getId())
                .title(title)
                .body(body)
                .itemType(ItemType.USER)
                .itemId(targetUser.getId())
                .notificationType(NotificationType.PARTNER_ACCEPTED)
                .build());
        for (NotificationDto.SendRequest request : requests) {
            createAndSendNotification(request.getUserId(), request, NotificationStrategy.IMMEDIATE,
                    null);
        }

    }

    @Override
    public void sendPartnerDeclined(User targetUser, User sentUser) {
        String title = PARTNER_DECLINED_TITLE.replace("${partnerName}", sentUser.getNickname());
        String body = "배우자 등록 신청이 거절되었습니다. 다시 시도해주세요!";

        NotificationDto.SendRequest request = NotificationDto.SendRequest.builder()
                .userId(targetUser.getId())
                .title(title)
                .body(body)
                .itemType(ItemType.USER)
                .itemId(sentUser.getId())
                .notificationType(NotificationType.PARTNER_DECLINED)
                .build();
        createAndSendNotification(targetUser.getId(), request, NotificationStrategy.IMMEDIATE,
                null);
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
        NotificationDto.SendRequest itemData = NotificationDto.SendRequest.builder()
                .userId(testUserId)
                .title(s)
                .body(s1)
                .itemType(ItemType.valueOf(itemType.get("itemType")))
                .itemId(Long.parseLong(itemType.get("itemId")))
                .notificationType(notificationType)
                .data(itemType)
                .build();
        createAndSendNotification(testUserId, itemData, notificationStrategy, LocalDateTime.now()
                .plusMinutes(1));
        log.debug("Test notification sent to user: {}, type: {}, title: {}, body: {}, itemData: {}",
                testUserId, notificationType, s, s1, itemData);

    }

    @Override
    public void notifyCommentDeleted(Comment comment) {
        Long targetUserId = comment.getUserId();
        String title = "서비스 규정을 위반하여 댓글이 삭제되었습니다";
        String body = String.format("게시글 '%s'의 댓글이 삭제되었습니다.", comment.getContent());

        NotificationDto.SendRequest request = NotificationDto.SendRequest.builder()
                .userId(targetUserId)
                .title(title)
                .body(body)
                .itemType(ItemType.COMMENT)
                .itemId(comment.getId())
                .notificationType(NotificationType.COMMENT_DELETED)
                .build();
        createAndSendNotification(targetUserId, request, NotificationStrategy.IMMEDIATE, null);

    }
}
