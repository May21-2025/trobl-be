package com.may21.trobl.admin.domain;

import com.may21.trobl._global.enums.PostingType;
import com.may21.trobl._global.utility.Utility;
import com.may21.trobl.post.domain.Posting;
import com.may21.trobl.redis.RedisDto;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.checkerframework.common.aliasing.qual.Unique;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
public class PostDetailInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Unique
    private Long postId;
    private PostingType postType;
    private String adminTags;
    private String tags;
    private long likeCount;
    private long commentCount;
    private long viewCount;
    private long voteCount;

    private String address;
    private int marriedYears;
    private int signedUpYears;

    private LocalDateTime createdAt;


    public PostDetailInfo(Posting post, RedisDto.UserDto userDto, String adminTags,
            List<String> tags) {
        this.postId = post.getId();
        this.postType = post.getPostType();
        this.adminTags = adminTags;
        this.tags = Utility.stringListToString(tags);
        this.likeCount = post.getPostLikes()
                .size();
        this.commentCount = post.getComments()
                .size();
        this.viewCount = 0;
        this.voteCount = 0;
        this.address = userDto == null ? "" : userDto.getAddress();
        this.marriedYears = userDto == null ? 0 : Utility.getYearCount(userDto.getMarriageDate());
        this.signedUpYears = userDto == null ? 0 : Utility.getYearCount(userDto.getSignedUpDate());
        this.createdAt = post.getCreatedAt();
    }

    public void update(int likeCount, int viewCount, int voteCount, int commentCount) {
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.viewCount = viewCount;
        this.voteCount = voteCount;
    }
}
