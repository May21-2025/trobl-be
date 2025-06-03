package com.may21.trobl.notification.service;

import com.may21.trobl.comment.dto.CommentDto;
import com.may21.trobl.notification.domain.Notification;
import com.may21.trobl.notification.dto.NotificationDto;
import com.may21.trobl.user.domain.User;

import java.time.LocalDateTime;
import java.util.Map;

public interface NotificationService {
  void sendImmediateNotification(Long userId, String title, String message, Map<String, String> data);

  void queueForBatchNotification(Long userId, Notification notification);

  void scheduleNotification(Long userId, Notification notification, LocalDateTime scheduledTime);

  void processBatchNotifications();

  void processScheduledNotifications();

  void sendPostLikeNotification(Long postId, Long userId);

  void sendCommentLikeNotification(Long commentId, Long userId);

  void sendNewCommentNotification(Long postId, CommentDto.Response commentDto);

  boolean markAsRead(Long notificationId, Long userId);

  void setNotificationSetting(User user);

  boolean setNotificationSettings(Long id, String notificationType, Boolean enabled);
}
