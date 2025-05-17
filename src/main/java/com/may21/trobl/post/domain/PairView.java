package com.may21.trobl.post.domain;

import com.may21.trobl.user.domain.User;
import jakarta.persistence.*;
import java.util.List;
import lombok.*;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class PairView {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  private Long id;

  @Setter private String title;
  @Setter private String content;

  private Long userId;

  @ManyToOne(fetch = FetchType.LAZY)
  private Posting posting;

  @Builder
  public PairView(String title, String content, Posting post, Long userId) {
    this.title = title;
    this.content = content;
    this.posting = post;
    this. userId=userId;
  }
}
