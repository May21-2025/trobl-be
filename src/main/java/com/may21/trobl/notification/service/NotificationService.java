package com.may21.trobl.notification.service;

import com.may21.trobl.comment.dto.CommentDto;
import com.may21.trobl.user.domain.User;

public interface NotificationService {
  void sendPostLikeNotification(Long postId, Long userId);

  void sendCommentLikeNotification(Long commentId, Long userId);

  void sendNewCommentNotification(Long postId, CommentDto.Response commentDto);

  boolean markAsRead(Long notificationId, Long userId);

  void setNotificationSetting(User user);
}
