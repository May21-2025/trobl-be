package com.may21.trobl.comment.controller;

import com.may21.trobl._global.Message;
import com.may21.trobl.comment.dto.CommentDto;
import com.may21.trobl.comment.service.CommentService;
import com.may21.trobl.notification.service.NotificationService;
import com.may21.trobl.report.ReportDto;
import com.may21.trobl.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/postings/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final NotificationService notificationService;

    @GetMapping("")
    public ResponseEntity<Message> getComments(@PathVariable Long postId, @AuthenticationPrincipal User user) {
        Long userId = user != null ? user.getId() : null;
        List<CommentDto.Response> response = commentService.getComments(postId, userId);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PostMapping("")
    public ResponseEntity<Message> createComment(@PathVariable Long postId,
                                                 @RequestBody CommentDto.Request request, @AuthenticationPrincipal User user) {
        CommentDto.Response response = commentService.createComment(postId, request, user.getId());
        notificationService.sendNewCommentNotification(postId, response);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<Message> updateComment(
            @RequestBody CommentDto.Request request,
            @PathVariable Long commentId,
            @AuthenticationPrincipal User user) {
        CommentDto.Response response = commentService.updateComment(user.getId(), request, commentId);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Message> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal User user) {
        boolean response = commentService.deleteComment(user.getId(), commentId);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PutMapping("/{commentId}/like")
    public ResponseEntity<Message> likeComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal User user) {
        CommentDto.Response response = commentService.likeComment(commentId, user.getId());
        if (response.isLiked()) notificationService.sendCommentLikeNotification(commentId, user.getId());
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PutMapping("/{commentId}/report")
    public ResponseEntity<Message> reportPost(
            @PathVariable Long commentId,
            @RequestBody ReportDto.Request reportRequest,
            @AuthenticationPrincipal User user) {
        boolean response = commentService.reportComment(user.getId(), commentId,reportRequest);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }
}
