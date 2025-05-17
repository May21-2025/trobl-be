package com.may21.trobl.notification.service;

import com.may21.trobl._global.enums.NotificationType;
import com.may21.trobl._global.exception.BusinessException;
import com.may21.trobl._global.exception.ExceptionCode;
import com.may21.trobl.comment.domain.Comment;
import com.may21.trobl.comment.domain.CommentRepository;
import com.may21.trobl.comment.dto.CommentDto;
import com.may21.trobl.notification.domain.Notification;
import com.may21.trobl.notification.domain.NotificationRepository;
import com.may21.trobl.notification.domain.NotificationSetting;
import com.may21.trobl.notification.domain.NotificationSettingRepository;
import com.may21.trobl.notification.dto.NotificationDto;
import com.may21.trobl.post.domain.PostRepository;
import com.may21.trobl.post.domain.Posting;
import com.may21.trobl.user.domain.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService{

    private final RedisTemplate<String, Object> redisTemplate;
    private final NotificationRepository notificationRepository;
    private final PushNotificationService pushNotificationService;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final NotificationSettingRepository notificationSettingRepository;

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

    private String toJson(Map<String, String> data) {
        try {
            return new ObjectMapper().writeValueAsString(data);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }


    @Override
    public void sendPostLikeNotification(Long postId, Long userId) {
        Posting post = postRepository.findById(postId)
                .orElse(null);
        if (post == null || post.getUserId().equals(userId)) {
            return;
        }

        Long receiverUserId = post.getUserId();

        // 2. Notification 객체 만들기
        Notification notification = Notification.builder()
                .userId(receiverUserId)
                .type(NotificationType.LIKE) // 좋아요 타입
                .title("새로운 좋아요 ✨")
                .body("당신의 게시글이 좋아요를 받았습니다!")
                .data(toJson(Map.of("postId", postId.toString())))
                .build();
        queueNotification(receiverUserId, notification);
    }

    @Override
    public void sendCommentLikeNotification(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElse(null);
        if (comment == null|| comment.getUserId().equals(userId)) {
            return;
        }

        Long receiverUserId = comment.getUserId();

        // 2. Notification 객체 만들기
        Notification notification = Notification.builder()
                .userId(receiverUserId)
                .type(NotificationType.LIKE) // 좋아요 타입
                .title("새로운 좋아요 ✨")
                .body("당신의 댓글이 좋아요를 받았습니다!")
                .data(toJson(Map.of("commentId", commentId.toString())))
                .build();
        queueNotification(receiverUserId, notification);
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
            commentSnippet = commentSnippet.substring(0, 20) + "..."; // 길이 자르기
        }

        // 3. Notification 객체 만들기
        Notification notification = Notification.builder()
                .userId(receiverUserId)
                .type(NotificationType.COMMENT)
                .title("새 댓글이 달렸어요! ✨")
                .body("“" + commentSnippet + "”")
                .data(toJson(Map.of(
                        "postId", postId.toString(),
                        "commentId", commentDto.getId().toString()
                )))
                .build();

        queueNotification(receiverUserId, notification);
    }

    @Override
    public boolean markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.NOTIFICATION_NOT_FOUND));
        if(!notification.getUserId()
                .equals(userId)) throw new BusinessException(ExceptionCode.FORBIDDEN)
            ;
        notification.markAsRead();
        return true;
    }

    @Override
    public void setNotificationSetting(User user) {
        NotificationSetting setting = new NotificationSetting(user);
        notificationSettingRepository.save(setting);
    }

    public void queueNotification(Long userId, Notification notification) {
        notificationRepository.save(notification);
        String key = "pending_notifications:" + userId;
        redisTemplate.opsForList().rightPush(key, notification.getId());
        redisTemplate.expire(key, Duration.ofMinutes(10));
    }
}