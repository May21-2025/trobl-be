package com.may21.trobl.post.domain;

import com.may21.trobl._global.enums.PostingType;
import com.may21.trobl.comment.domain.Comment;
import jakarta.persistence.*;
import lombok.*;
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
  private String pollTitle;

  @Setter
  @OneToMany(mappedBy = "posting", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private List<PollOption> pollOptions;

  @Setter
  @OneToMany(mappedBy = "posting", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private List<PairView> pairViews;

  @Setter
  @OneToMany(mappedBy = "posting", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private List<PostLike> postLikes;

  @OneToMany(mappedBy = "posting", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private List<Comment> comments;

  @Setter private int viewCount;

  @Setter private int shareCount;

  @CreatedDate private LocalDateTime createdAt;

  @Builder
  public Posting(
      String title, String pollTitle, PostingType postType, String content, Long userId, String nickname) {
    super(title, content, userId);
    this.nickname = nickname;
    this.pollTitle = pollTitle;
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
}
