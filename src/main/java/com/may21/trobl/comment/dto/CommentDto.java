package com.may21.trobl.comment.dto;

import com.may21.trobl.comment.domain.Comment;
import com.may21.trobl.notification.dto.NotificationDto;
import com.may21.trobl.post.domain.Posting;
import com.may21.trobl.redis.RedisDto;
import com.may21.trobl.user.UserDto;
import com.may21.trobl.user.domain.User;
import lombok.Getter;

import java.time.LocalDateTime;

import static com.may21.trobl._global.utility.SecurityUtils.decodeHtml;

public class CommentDto {

    @Getter
    public static class Request {
        private String content;
        private Long commentId;
    }

    @Getter
    public static class Response {
        private final Long commentId;
        private final String content;
        private final UserDto.Info user;
        private final Long parentCommentId;
        private final Long postId;
        private final LocalDateTime createdAt;
        private final int likeCount;
        private final boolean liked;

        public Response(Comment comment, User user, boolean liked) {
            this.commentId = comment.getId();
            this.content = decodeHtml(comment.getContent());
            this.user = user != null ? new UserDto.Info(user) : null;
            this.postId = comment.getPosting()
                    .getId();
            this.parentCommentId = comment.getParentComment() != null ? comment.getParentComment()
                    .getId() : null;
            this.createdAt = comment.getCreatedAt();
            this.likeCount = comment.getLikeCount();
            this.liked = liked;
        }

        public Response(Comment comment, RedisDto.UserDto userDto, boolean liked) {
            this.commentId = comment.getId();
            this.content = decodeHtml(comment.getContent());
            this.user = new UserDto.Info(userDto);
            this.postId = comment.getPosting()
                    .getId();
            this.parentCommentId = comment.getParentComment() != null ? comment.getParentComment()
                    .getId() : null;
            this.createdAt = comment.getCreatedAt();
            this.likeCount = comment.getLikeCount();
            this.liked = liked;
        }
    }

    @Getter
    public static class MyComments extends Response {
        private final String postTitle;
        private final boolean unread;
        private final boolean newComment;
        private final boolean newLike;

        public MyComments(Posting post, Comment comment, User user, boolean liked,
                NotificationDto.ContentUpdateStatus contentUpdateStatus) {
            super(comment, user, liked);
            this.postTitle = post.getTitle();
            this.unread = contentUpdateStatus.isUnread();
            this.newComment = contentUpdateStatus.isNewComment();
            this.newLike = contentUpdateStatus.isNewLike();
        }
    }
}
