package com.may21.trobl.redis;

import com.may21.trobl._global.enums.PostingType;
import com.may21.trobl.post.domain.Posting;
import com.may21.trobl.user.domain.User;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serial;
import java.io.Serializable;

public class RedisDto {

    @RedisHash(value = "post_cache", timeToLive = 3600) // 1 hour TTL
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PostDto implements Serializable {
        @Serial
        private static final long serialVersionUID = -214490344996507077L;

        @Id
        private Long postId;
        private String title;
        private Long userId;
        private String content;
        private PostingType postType;
        private String createdAt; // LocalDateTime을 String으로 변경
        private boolean confirmed;
        private int viewCount;
        private int shareCount;
        private boolean reported;

        public PostDto(Posting posting) {
            this.postId = posting.getId();
            this.title = posting.getTitle();
            this.userId = posting.getUserId();
            this.content = posting.getContent();
            this.postType = posting.getPostType();
            this.createdAt = posting.getCreatedAt() != null ? posting.getCreatedAt()
                    .toString() : null;
            this.confirmed = posting.isConfirmed();
            this.viewCount = posting.getViewCount();
            this.shareCount = posting.getShareCount();
            this.reported = posting.isReported();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PollDto implements Serializable {
        @Serial
        private static final long serialVersionUID = -214490344996507077L;
        private Long postId;
        private Long pollId;
        private boolean allowMultipleVotes;
        private String title;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PollOptionDto implements Serializable {
        @Serial
        private static final long serialVersionUID = -214490344996507077L;
        private Long pollOptionId;
        private Long pollId;
        private Long postId;
        private String name;
        private int index;
        private int voteCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FairViewDto implements Serializable {
        @Serial
        private static final long serialVersionUID = -214490344996507077L;
        private Long postId;
        private Long fairViewId;
        private Long userId;
        private String title;
        private String nickname;
        private String content;
        private boolean confirmed;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TagDto implements Serializable {
        @Serial
        private static final long serialVersionUID = -214490344996507077L;
        private Long tagId;
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDto implements Serializable {
        @Serial
        private static final long serialVersionUID = -214490344996507077L;
        private Long userId;
        private String username;
        private String nickname;
        private String thumbnailUrl;
        private String address;
        private String marriageDate;
        private String signedUpDate;

        public UserDto(User user) {
            if (user == null) {
                return;
            }
            this.userId = user.getId();
            this.username = user.getUsername();
            this.nickname = user.getNickname();
            this.thumbnailUrl = user.getThumbnailUrl();
            this.address = user.getAddress();
            this.marriageDate = user.getWeddingAnniversaryDate() == null ? null :
                    user.getWeddingAnniversaryDate()
                            .toString();
            this.signedUpDate = user.getSignUpDate()
                    .toString();

        }
    }


}
