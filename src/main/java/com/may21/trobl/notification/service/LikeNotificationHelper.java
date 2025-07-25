package com.may21.trobl.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 좋아요 알림 관련 헬퍼 서비스
 * 좋아요/취소 시 알림 처리를 담당
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LikeNotificationHelper {

    private final NotificationBatchService notificationBatchService;

    /**
     * 포스트 좋아요 시 알림 추가
     */
    public void onPostLiked(Long postId, Long userId) {
        notificationBatchService.addPostLikeToQueue(postId, userId);
    }

    /**
     * 포스트 좋아요 취소 시 알림 제거
     */
    public void onPostUnliked(Long postId, Long userId) {
        notificationBatchService.removePostLikeFromQueue(postId, userId);
    }

    /**
     * 댓글 좋아요 시 알림 추가
     */
    public void onCommentLiked(Long commentId, Long userId) {
        notificationBatchService.addCommentLikeToQueue(commentId, userId);
    }

    /**
     * 댓글 좋아요 취소 시 알림 제거
     */
    public void onCommentUnliked(Long commentId, Long userId) {
        notificationBatchService.removeCommentLikeFromQueue(commentId, userId);
    }

    /**
     * 현재 대기 중인 포스트 좋아요 알림 개수 조회
     */
    public int getPendingPostLikeCount(Long postId) {
        return notificationBatchService.getPendingPostLikeCount(postId);
    }

    /**
     * 현재 대기 중인 댓글 좋아요 알림 개수 조회
     */
    public int getPendingCommentLikeCount(Long commentId) {
        return notificationBatchService.getPendingCommentLikeCount(commentId);
    }
}
