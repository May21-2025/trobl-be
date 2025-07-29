package com.may21.trobl.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.may21.trobl._global.enums.ItemType;
import com.may21.trobl._global.enums.NotificationType;
import com.may21.trobl.comment.domain.Comment;
import com.may21.trobl.comment.domain.CommentRepository;
import com.may21.trobl.notification.domain.Notification;
import com.may21.trobl.notification.domain.NotificationRepository;
import com.may21.trobl.notification.dto.NotificationDto;
import com.may21.trobl.post.domain.PostRepository;
import com.may21.trobl.post.domain.Posting;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 좋아요 알림 배치 처리 전용 서비스
 * 같은 항목(포스트/댓글)의 좋아요들을 10분마다 묶어서 전송
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationBatchService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final NotificationRepository notificationRepository;
    private final PushNotificationService pushNotificationService;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Redis 키 패턴
    private static final String BATCH_LIKE_KEY_PATTERN = "batch_likes:user:%d:item:%s:%d";
    private static final String ACTOR_SET_KEY_SUFFIX = ":actors";

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BatchLikeData {
        private Long actorUserId;
        private String actorUserName;
        private Long timestamp;
        private String notificationId;

        public BatchLikeData(Long actorUserId, String actorUserName, Long notificationId) {
            this.actorUserId = actorUserId;
            this.actorUserName = actorUserName;
            this.timestamp = System.currentTimeMillis();
            this.notificationId = notificationId.toString();
        }
    }

    /**
     * 포스트 좋아요 알림을 배치 큐에 추가 (비동기)
     */
    @Async("notificationTaskExecutor")
    @Transactional
    public void addPostLikeToQueue(Long postId, Long actorUserId) {
        log.debug("Adding post like to queue asynchronously: postId={}, actorUserId={}", postId,
                actorUserId);

        User actor = userRepository.findById(actorUserId)
                .orElse(null);
        if (actor == null || actor.isNotificationBlocked(NotificationType.LIKE)) {
            return;
        }

        Posting post = postRepository.findById(postId)
                .orElse(null);
        if (post == null || post.getUserId()
                .equals(actorUserId)) {
            return; // 자신의 포스트에는 알림 안 보냄
        }

        addLikeToQueue(post.getUserId(), actorUserId, actor.getNickname(), ItemType.POST, postId);
    }

    /**
     * 댓글 좋아요 알림을 배치 큐에 추가 (비동기)
     */
    @Async("notificationTaskExecutor")
    @Transactional
    public void addCommentLikeToQueue(Long commentId, Long actorUserId) {
        log.debug("Adding comment like to queue asynchronously: commentId={}, actorUserId={}",
                commentId, actorUserId);

        Comment comment = commentRepository.findById(commentId)
                .orElse(null);
        if (comment == null || comment.getUserId()
                .equals(actorUserId)) {
            return; // 자신의 댓글에는 알림 안 보냄
        }

        User actor = userRepository.findById(actorUserId)
                .orElse(null);
        if (actor == null || actor.isNotificationBlocked(NotificationType.LIKE)) {
            return;
        }

        addLikeToQueue(comment.getUserId(), actorUserId, actor.getNickname(), ItemType.COMMENT,
                commentId);
    }

    /**
     * 좋아요 알림을 배치 큐에 추가하는 공통 메서드
     */
    private void addLikeToQueue(Long recipientUserId, Long actorUserId, String actorUserName,
            ItemType itemType, Long itemId) {
        NotificationType notificationType = NotificationType.LIKE;
        String batchKey = String.format(BATCH_LIKE_KEY_PATTERN, recipientUserId, itemType.name()
                .toLowerCase(), itemId);
        String actorSetKey = batchKey + ACTOR_SET_KEY_SUFFIX;

        // 중복 체크: 같은 사용자가 같은 아이템에 이미 좋아요했는지 확인
        Long addResult = redisTemplate.opsForSet()
                .add(actorSetKey, actorUserId.toString());
        boolean isNewLike = addResult != null && addResult > 0;

        if (isNewLike) {
            // 새로운 좋아요인 경우에만 처리
            // 1. DB에 알림 저장
            NotificationDto.SendRequest request = NotificationDto.SendRequest.builder()
                    .userId(recipientUserId)
                    .title(getBasicLikeTitle(itemType))
                    .body(getBasicLikeBody(itemType, actorUserName))
                    .itemType(itemType)
                    .itemId(itemId)
                    .notificationType(notificationType)
                    .build();
            Notification notification = new Notification(recipientUserId, request, null);
            notificationRepository.save(notification);

            // 2. 배치 처리를 위한 Redis 큐에 추가
            BatchLikeData likeData =
                    new BatchLikeData(actorUserId, actorUserName, notification.getId());

            try {
                String likeDataJson = objectMapper.writeValueAsString(likeData);
                redisTemplate.opsForList()
                        .rightPush(batchKey, likeDataJson);

                // 만료 시간 설정 (15분)
                redisTemplate.expire(batchKey, Duration.ofMinutes(15));
                redisTemplate.expire(actorSetKey, Duration.ofMinutes(15));

                log.debug("Added like to batch queue: recipient={}, actor={}, item={}:{}",
                        recipientUserId, actorUserId, itemType, itemId);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize like data for batch processing", e);
                // JSON 직렬화 실패 시 알림 삭제
                notificationRepository.deleteById(notification.getId());
            }
        }
        else {
            log.debug("Duplicate like ignored: recipient={}, actor={}, item={}:{}", recipientUserId,
                    actorUserId, itemType, itemId);
        }
    }

    /**
     * 포스트 좋아요 취소 시 큐에서 제거 (비동기)
     */
    @Async("notificationTaskExecutor")
    @Transactional
    public void removePostLikeFromQueue(Long postId, Long actorUserId) {
        log.debug("Removing post like from queue asynchronously: postId={}, actorUserId={}", postId,
                actorUserId);

        Posting post = postRepository.findById(postId)
                .orElse(null);
        if (post == null) {
            return;
        }

        removeLikeFromQueue(post.getUserId(), actorUserId, ItemType.POST, postId);
    }

    /**
     * 댓글 좋아요 취소 시 큐에서 제거 (비동기)
     */
    @Async("notificationTaskExecutor")
    @Transactional
    public void removeCommentLikeFromQueue(Long commentId, Long actorUserId) {
        log.debug("Removing comment like from queue asynchronously: commentId={}, actorUserId={}",
                commentId, actorUserId);

        Comment comment = commentRepository.findById(commentId)
                .orElse(null);
        if (comment == null) {
            return;
        }

        removeLikeFromQueue(comment.getUserId(), actorUserId, ItemType.COMMENT, commentId);
    }

    /**
     * 좋아요 취소 시 배치 큐에서 제거하는 공통 메서드
     */
    private void removeLikeFromQueue(Long recipientUserId, Long actorUserId, ItemType itemType,
            Long itemId) {
        String batchKey = String.format(BATCH_LIKE_KEY_PATTERN, recipientUserId, itemType.name()
                .toLowerCase(), itemId);
        String actorSetKey = batchKey + ACTOR_SET_KEY_SUFFIX;

        // 중복 체크 Set에서 제거
        redisTemplate.opsForSet()
                .remove(actorSetKey, actorUserId.toString());

        // 리스트에서 해당 사용자의 좋아요 데이터 찾아서 제거
        List<Object> likeDataList = redisTemplate.opsForList()
                .range(batchKey, 0, -1);
        if (likeDataList != null) {
            for (int i = 0; i < likeDataList.size(); i++) {
                try {
                    BatchLikeData likeData = objectMapper.readValue(likeDataList.get(i)
                            .toString(), BatchLikeData.class);
                    if (likeData.getActorUserId()
                            .equals(actorUserId)) {
                        // 해당 항목을 찾았으면 제거
                        redisTemplate.opsForList()
                                .set(batchKey, i, "REMOVED");
                        redisTemplate.opsForList()
                                .remove(batchKey, 1, "REMOVED");

                        // 알림 DB에서도 제거
                        if (likeData.getNotificationId() != null) {
                            notificationRepository.deleteById(
                                    Long.parseLong(likeData.getNotificationId()));
                        }

                        log.debug(
                                "Removed like from batch queue: recipient={}, actor={}, item={}:{}",
                                recipientUserId, actorUserId, itemType, itemId);
                        break;
                    }
                } catch (JsonProcessingException e) {
                    log.error("Failed to parse like data during removal", e);
                }
            }
        }
    }

    /**
     * 스케줄러에서 호출: 배치 좋아요 알림 처리
     */
    @Transactional
    public void processBatchLikeNotifications() {
        Set<String> likeKeys = redisTemplate.keys("batch_likes:user:*");

        if (likeKeys.isEmpty()) {
            log.debug("No batch like notifications to process");
            return;
        }

        log.debug("Processing {} batch like notification groups", likeKeys.size());

        Map<Long, List<String>> userLikeKeys = new HashMap<>();

        // 사용자별로 좋아요 키 그룹화
        for (String key : likeKeys) {
            try {
                // batch_likes:user:123:item:post:456 에서 userId 추출
                String[] parts = key.split(":");
                if (parts.length >= 3) {
                    Long userId = Long.parseLong(parts[2]);
                    userLikeKeys.computeIfAbsent(userId, k -> new ArrayList<>())
                            .add(key);
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid user ID in batch like key: {}", key);
            }
        }

        // 각 사용자별로 좋아요 알림 처리 (비동기)
        int processedUsers = 0;
        for (Map.Entry<Long, List<String>> entry : userLikeKeys.entrySet()) {
            Long userId = entry.getKey();
            List<String> keys = entry.getValue();

            processBatchLikeNotificationsForUserAsync(userId, keys);
            processedUsers++;
        }

        log.debug("Started processing batch like notifications for {} users", processedUsers);
    }

    /**
     * 특정 사용자의 배치 좋아요 알림 처리 (비동기)
     */
    @Async("notificationTaskExecutor")
    public void processBatchLikeNotificationsForUserAsync(Long userId, List<String> likeKeys) {
        User user = userRepository.findById(userId)
                .orElse(null);
        if (user == null || user.getFcmTokenList()
                .isEmpty()) {
            // 사용자가 없거나 FCM 토큰이 없으면 해당 키들 정리
            cleanupKeys(likeKeys);
            log.warn("User {} not found or has no FCM tokens, cleaned up {} keys", userId,
                    likeKeys.size());
            return;
        }

        int processedItems = 0;
        for (String likeKey : likeKeys) {
            List<Object> likeDataList = redisTemplate.opsForList()
                    .range(likeKey, 0, -1);

            if (likeDataList == null || likeDataList.isEmpty()) {
                continue;
            }

            List<BatchLikeData> likes = new ArrayList<>();
            for (Object obj : likeDataList) {
                try {
                    BatchLikeData likeData =
                            objectMapper.readValue(obj.toString(), BatchLikeData.class);
                    likes.add(likeData);
                } catch (JsonProcessingException e) {
                    log.error("Failed to parse batch like data: {}", obj.toString(), e);
                }
            }

            if (!likes.isEmpty()) {
                sendBatchLikeNotificationAsync(user, likeKey, likes);
                processedItems++;
            }

            // 처리 완료 후 Redis 데이터 정리
            redisTemplate.delete(likeKey);
            redisTemplate.delete(likeKey + ACTOR_SET_KEY_SUFFIX);
        }

        log.debug("Processed {} like notification groups for user {}", processedItems, userId);
    }

    /**
     * 묶인 좋아요 알림 전송 (비동기)
     */
    @Async("notificationTaskExecutor")
    public void sendBatchLikeNotificationAsync(User user, String likeKey,
            List<BatchLikeData> likes) {
        // 키에서 아이템 정보 추출: batch_likes:user:123:item:post:456
        String[] keyParts = likeKey.split(":");
        if (keyParts.length < 6) {
            log.warn("Invalid like key format: {}", likeKey);
            return;
        }

        String itemTypeStr = keyParts[4]; // "post" or "comment"
        ItemType itemType = ItemType.fromString(itemTypeStr);
        String itemId = keyParts[5];   // "456"

        // 최근 좋아요한 사람들 이름 (최대 3명, 시간순 정렬)
        List<String> actorNames = likes.stream()
                .sorted((a, b) -> b.getTimestamp()
                        .compareTo(a.getTimestamp()))
                .limit(3)
                .map(BatchLikeData::getActorUserName)
                .collect(Collectors.toList());

        String title = generateBatchLikeTitle(itemTypeStr, likes.size());
        String body = generateBatchLikeBody(itemTypeStr, actorNames, likes.size());

        Map<String, String> data = new HashMap<>();
        data.put("count", String.valueOf(likes.size()));

        // FCM 푸시 알림 전송
        NotificationDto.SendRequest request = NotificationDto.SendRequest.builder()
                .userId(user.getId())
                .title(title)
                .body(body)
                .itemId(Long.parseLong(itemId))
                .itemType(itemType)
                .notificationType(NotificationType.LIKE)
                .data(data)
                .build();

        try {
            pushNotificationService.sendNotificationTo(user.getFcmTokenList(), request);
            log.debug("Sent batch like notification to user {}: {} likes on {} {}", user.getId(),
                    likes.size(), itemTypeStr, itemId);
        } catch (Exception e) {
            log.error("Failed to send batch like notification to user {}", user.getId(), e);
        }
    }

    private String generateBatchLikeTitle(String itemType, int likeCount) {
        return switch (itemType.toLowerCase()) {
            case "post" -> likeCount == 1 ? "게시물에 좋아요를 받았습니다" : "게시물에 여러 좋아요를 받았습니다";
            case "comment" -> likeCount == 1 ? "댓글에 좋아요를 받았습니다" : "댓글에 여러 좋아요를 받았습니다";
            default -> "좋아요를 받았습니다";
        };
    }

    private String generateBatchLikeBody(String itemType, List<String> actorNames, int totalCount) {
        String itemTypeKorean = itemType.equals("post") ? "게시물" : "댓글";

        if (totalCount == 1) {
            return actorNames.getFirst() + "님이 회원님의 " + itemTypeKorean + "에 좋아요를 눌렀습니다.";
        }
        else {
            int othersCount = totalCount - 2;
            return actorNames.get(0) + "님, " + actorNames.get(1) + "님 외 " + othersCount +
                    "명이 회원님의 " + itemTypeKorean + "에 좋아요를 눌렀습니다.";
        }
    }


    private String getBasicLikeTitle(ItemType itemType) {
        return switch (itemType) {
            case POST -> "게시물에 좋아요를 받았습니다";
            case COMMENT -> "댓글에 좋아요를 받았습니다";
            default -> "좋아요를 받았습니다";
        };
    }

    private String getBasicLikeBody(ItemType itemType, String actorName) {
        return switch (itemType) {
            case POST -> actorName + "님이 회원님의 게시물에 좋아요를 눌렀습니다.";
            case COMMENT -> actorName + "님이 회원님의 댓글에 좋아요를 눌렀습니다.";
            default -> actorName + "님이 좋아요를 눌렀습니다.";
        };
    }

    private void cleanupKeys(List<String> keys) {
        for (String key : keys) {
            redisTemplate.delete(key);
            redisTemplate.delete(key + ACTOR_SET_KEY_SUFFIX);
        }
    }

    /**
     * 현재 대기 중인 좋아요 개수 조회 (포스트)
     */
    public int getPendingPostLikeCount(Long postId) {
        Posting post = postRepository.findById(postId)
                .orElse(null);
        if (post == null) {
            return 0;
        }

        String batchKey = String.format(BATCH_LIKE_KEY_PATTERN, post.getUserId(),
                ItemType.POST.name()
                        .toLowerCase(), postId);

        Long count = redisTemplate.opsForList()
                .size(batchKey);
        return count != null ? count.intValue() : 0;
    }

    /**
     * 현재 대기 중인 좋아요 개수 조회 (댓글)
     */
    public int getPendingCommentLikeCount(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElse(null);
        if (comment == null) {
            return 0;
        }

        String batchKey = String.format(BATCH_LIKE_KEY_PATTERN, comment.getUserId(),
                ItemType.COMMENT.name()
                        .toLowerCase(), commentId);

        Long count = redisTemplate.opsForList()
                .size(batchKey);
        return count != null ? count.intValue() : 0;
    }
}
