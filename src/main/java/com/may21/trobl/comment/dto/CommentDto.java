package com.may21.trobl.comment.dto;

import com.may21.trobl.comment.domain.Comment;
import com.may21.trobl.post.domain.Posting;
import com.may21.trobl.user.domain.User;
import lombok.Getter;

import java.time.LocalDateTime;

public class CommentDto {

    @Getter
    public static class Request {
        private String content;
        private Long commentId;
    }

    @Getter
    public static class Response {
        private Long id;
        private String content;
        private Long userId;
        private Long parentCommentId;
        private Long postId;
        private String nickname;
        private LocalDateTime createdAt;
        private int likeCount;
        private boolean liked;

        public Response(Comment comment, User user, boolean liked) {
            this.id = comment.getId();
            this.content = comment.getContent();
            this.userId = user.getId();
            this.nickname = user.getNickname();
            this.postId = comment.getPosting().getId();
            this.parentCommentId = comment.getParentComment() != null ? comment.getParentComment().getId() : null;
            this.createdAt = comment.getCreatedAt();
            this.likeCount = comment.getLikeCount();
            this.liked = liked;
        }
    }

    public static class RecentInfo extends Response {
        private String postTitle;

        public RecentInfo(Posting post, Comment comment, User user, boolean liked) {
            super(comment, user, liked);
            this.postTitle = post.getTitle();
        }
    }
}
