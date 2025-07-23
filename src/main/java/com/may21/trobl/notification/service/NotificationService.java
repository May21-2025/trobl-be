package com.may21.trobl.notification.service;

import com.may21.trobl.admin.AdminDto;
import com.may21.trobl.comment.dto.CommentDto;
import com.may21.trobl.notification.domain.Notification;
import com.may21.trobl.notification.dto.NotificationDto;
import com.may21.trobl.post.dto.PostDto;
import com.may21.trobl.user.domain.User;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

public interface NotificationService {
    void sendImmediateNotification(Long userId, String title, String message, Map<String, String> data);

    void queueForBatchNotification(Long userId, Notification notification);

    void scheduleNotification(Long userId, Notification notification, LocalDateTime scheduledTime);

    void processBatchNotifications();

    void processScheduledNotifications();

    @Transactional
    void sendPostLikeNotification(Long postId, Long userId);

    @Transactional
    void sendCommentLikeNotification(Long commentId, Long userId);

    @Transactional
    void sendNewCommentNotification(Long postId, CommentDto.Response commentDto);

    @Transactional
    boolean markAsRead(Long notificationId, Long userId);

    @Transactional
    void setNotificationSetting(User user);

    @Transactional
    boolean setNotificationSettings(Long id, String notificationType, Boolean enabled);

    @Transactional
    void notifyPostDeleted(Long userId, PostDto.Notification info);

    @Transactional
    boolean notifyMarketingAlert(AdminDto.PushNotification message);

    @Transactional
    void sendFairViewRequest(Long id, User partner);

    @Transactional
    void sendFairViewConfirmedRequest(Long fairViewId, Long userId);

    boolean getMainNotification(Long userId);


    NotificationDto.SubMenu getSubManuNotification(Long userId);

    @Transactional
    boolean readAllNotifications(Long userId);
}
