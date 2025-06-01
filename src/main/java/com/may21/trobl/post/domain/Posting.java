package com.may21.trobl.post.domain;

import com.may21.trobl._global.enums.PostingType;
import com.may21.trobl.comment.domain.Comment;
import com.may21.trobl.tag.domain.TagMapping;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class Posting extends ContentEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  private Long id;

  private String nickname;

  private PostingType postType;

  @Setter
  @OneToOne(mappedBy = "posting", cascade = CascadeType.ALL, orphanRemoval = true)
  private Poll poll;

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


  @Setter private int viewCount;

  @Setter private int shareCount;

  @CreatedDate private LocalDateTime createdAt;

  @Builder
  public Posting(
      String title, String pollTitle, PostingType postType, String content, Long userId, String nickname) {
    super(title, content, userId);
    this.nickname = nickname;
    this.postType = postType;
    this.viewCount = 0;
    this.shareCount = 0;
  }

  public void incrementViewCount() {
    this.viewCount++;
  }

  public void incrementShareCount() {
    this.shareCount++;
  }

  public int getViewCount() {
    int viewsNum = views==null ? 0 : views.size();
    return viewCount+ viewsNum;
  }

  public void addFairView(FairView fairView) {
    if(fairView == null) {
      throw new IllegalArgumentException("FairView cannot be null");
    }
    this.fairViews.add(fairView);
  }

}
