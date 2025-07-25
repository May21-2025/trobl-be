package com.may21.trobl.notification.controller;

import com.may21.trobl.notification.service.LikeNotificationHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 알림 관련 컨트롤러 예제
 * 실제 좋아요 기능과 연동하여 사용
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationExampleController {

    private final LikeNotificationHelper likeNotificationHelper;

    /**
     * 포스트 좋아요 토글 예제
     * 실제로는 PostController 또는 LikeController에서 호출
     */
    @PostMapping("/like/post/{postId}")
    public ResponseEntity<String> togglePostLike(
            @PathVariable Long postId,
            @RequestParam Long userId,
            @RequestParam boolean isLike) {
        
        if (isLike) {
            // 좋아요 추가 시 (비동기 처리)
            likeNotificationHelper.onPostLiked(postId, userId);
            return ResponseEntity.ok("포스트 좋아요 알림이 비동기로 큐에 추가되었습니다.");
        } else {
            // 좋아요 취소 시 (비동기 처리)
            likeNotificationHelper.onPostUnliked(postId, userId);
            return ResponseEntity.ok("포스트 좋아요 알림이 비동기로 큐에서 제거되었습니다.");
        }
    }

    /**
     * 댓글 좋아요 토글 예제
     */
    @PostMapping("/like/comment/{commentId}")
    public ResponseEntity<String> toggleCommentLike(
            @PathVariable Long commentId,
            @RequestParam Long userId,
            @RequestParam boolean isLike) {
        
        if (isLike) {
            // 좋아요 추가 시 (비동기 처리)
            likeNotificationHelper.onCommentLiked(commentId, userId);
            return ResponseEntity.ok("댓글 좋아요 알림이 비동기로 큐에 추가되었습니다.");
        } else {
            // 좋아요 취소 시 (비동기 처리)
            likeNotificationHelper.onCommentUnliked(commentId, userId);
            return ResponseEntity.ok("댓글 좋아요 알림이 비동기로 큐에서 제거되었습니다.");
        }
    }

    /**
     * 대기 중인 포스트 좋아요 알림 개수 조회
     */
    @GetMapping("/pending/post/{postId}")
    public ResponseEntity<Integer> getPendingPostLikeCount(@PathVariable Long postId) {
        int count = likeNotificationHelper.getPendingPostLikeCount(postId);
        return ResponseEntity.ok(count);
    }

    /**
     * 대기 중인 댓글 좋아요 알림 개수 조회
     */
    @GetMapping("/pending/comment/{commentId}")
    public ResponseEntity<Integer> getPendingCommentLikeCount(@PathVariable Long commentId) {
        int count = likeNotificationHelper.getPendingCommentLikeCount(commentId);
        return ResponseEntity.ok(count);
    }
}
