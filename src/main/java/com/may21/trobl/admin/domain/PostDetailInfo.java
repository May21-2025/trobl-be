package com.may21.trobl.admin.domain;

import com.may21.trobl._global.enums.PostingType;
import com.may21.trobl._global.utility.Utility;
import com.may21.trobl.post.domain.Posting;
import com.may21.trobl.redis.RedisDto;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
public class PostDetailInfo {
    @Id
    private Long postId;
    private PostingType postType;
    private String tagMappingIds;
    private long likeCount;
    private long commentCount;
    private long viewCount;
    private long voteCount;

    private String address;
    private int marriedYears;
    private int signedUpYears;

    private LocalDateTime createdAt;


    public List<Long> getTagMappingIds(){
        if (this.tagMappingIds == null) {
            return new ArrayList<>();
        }
        return Utility.stringToLongList(this.tagMappingIds);
    }

    public PostDetailInfo(Posting post, RedisDto.UserDto userDto, List<Long> tagMappingIds) {
        this.postId = post.getId();
        this.postType = post.getPostType();
        this.tagMappingIds = Utility.longListToString(tagMappingIds);
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
