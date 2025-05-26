package com.may21.trobl.post.domain;

import com.may21.trobl.user.domain.User;
import jakarta.persistence.*;
import java.util.List;
import lombok.*;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class FairView {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  private Long id;

  @Setter private String title;

  @Column(columnDefinition = "text")
  @Setter private String content;

  private Long userId;
  @Setter
    private String nickname;

  @ManyToOne(fetch = FetchType.LAZY)
  private Posting posting;

  private boolean confirmed;

  @Builder
  public FairView(String title, String content, String nickname, Posting post, Long userId) {
    this.title = title;
    this.content = content;
    this.posting = post;
    this. userId=userId;
    this.nickname=nickname;
    this.confirmed = false;
  }
}
