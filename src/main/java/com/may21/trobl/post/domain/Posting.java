package com.may21.trobl.post.domain;

import com.may21.trobl._global.enums.PostingType;
import com.may21.trobl._global.exception.BusinessException;
import com.may21.trobl._global.exception.ExceptionCode;
import com.may21.trobl.bookmark.PostBookmark;
import com.may21.trobl.comment.domain.Comment;
import com.may21.trobl.poll.domain.Poll;
import com.may21.trobl.post.dto.PostDto;
import com.may21.trobl.tag.domain.TagMapping;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class Posting {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Setter
    private String nickname;

    private Long userId;

    private String title;

    @Column(columnDefinition = "text")
    private String content;

    private PostingType postType;

    @OneToMany(mappedBy = "posting", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Poll> poll;

    @Setter
    @BatchSize(size = 10)
    @OneToMany(mappedBy = "posting", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FairView> fairViews;

    @Setter
    @BatchSize(size = 10)
    @OneToMany(mappedBy = "posting", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PostLike> postLikes;

    @BatchSize(size = 10)
    @OneToMany(mappedBy = "posting", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Comment> comments;

    @BatchSize(size = 10)
    @OneToMany(mappedBy = "posting", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PostView> views;

    @BatchSize(size = 10)
    @OneToMany(mappedBy = "posting", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PostBookmark> bookmarks;

    @Setter
    @BatchSize(size = 10)
    @OneToMany(mappedBy = "posting", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TagMapping> tags;


    @Setter
    private int viewCount;

    @Setter
    private int shareCount;

    @CreatedDate
    private LocalDateTime createdAt;

    @Setter
    private Boolean confirmed;

    @Setter
    private Boolean reported;

    @Builder
    public Posting(
            String title, PostingType postType, String content, Long userId, String nickname) {
        this.userId = userId;
        this.title = title;
        this.content = content;

        this.nickname = nickname;
        this.postType = postType;
        this.viewCount = 0;
        this.shareCount = 0;
        this.confirmed = postType != PostingType.FAIR_VIEW;
        this.reported = false;
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public void incrementShareCount() {
        this.shareCount++;
    }

    public int getViewCount() {
        int viewsNum = views == null ? 0 : views.size();
        return viewCount + viewsNum;
    }

    public void addFairView(FairView fairView) {
        if (fairView == null) {
            throw new IllegalArgumentException("FairView cannot be null");
        }
        if (this.fairViews == null) {
            this.fairViews = new ArrayList<>();
        }
        this.fairViews.add(fairView);
    }

    public Poll getPoll() {
        if (poll == null || poll.isEmpty()) {
            return null;
        }
        if (poll.size() > 1) {
            //delete all except the first one
            for (int i = 1; i < poll.size(); i++) {
                Poll p = poll.get(i);
                p.setPosting(null);
            }
            poll = poll.subList(0, 1);
        }
        return poll.getFirst();
    }

    public void setPoll(Poll poll) {
        //if this.poll has one throw exception
        if (this.poll != null && !this.poll.isEmpty()) {
            throw new BusinessException(ExceptionCode.FORBIDDEN, "Posting already has a poll");
        }
        if (poll == null) {
            this.poll = null;
        } else {
            this.poll = List.of(poll);
            poll.setPosting(this);
        }
    }

    public List<Comment> getComments() {
        if (comments == null) {
            return new ArrayList<>();
        }
        return comments;
    }

    public List<PostLike> getPostLikes() {
        if (postLikes == null) {
            return new ArrayList<>();
        }
        return postLikes;
    }

    public List<TagMapping> getTags() {
        if (tags == null) {
            return new ArrayList<>();
        }
        return tags;
    }

    public void update(PostDto.Request request) {
        if (request.getTitle() != null) {
            this.title = request.getTitle();
        }
        if (request.getContent() != null) {
            this.content = request.getContent();
        }
    }

    public int getLikeCount() {
        if (postLikes == null) {
            return 0;
        }
        return postLikes.size();
    }

    public int getCommentCount() {
        if (comments == null) {
            return 0;
        }
        return comments.size();
    }

    public boolean isReported() {
        return reported != null && reported;
    }
}
