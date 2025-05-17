package com.may21.trobl.post.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@EntityListeners(AuditingEntityListener.class)
@Table(uniqueConstraints = @UniqueConstraint(name = "unique_user_post", columnNames = {"userId", "posting_id"}))
public class PostLike {

  @Id
  @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
  private Long id;

  private Long userId;

  @ManyToOne private Posting posting;
  
  @CreatedDate
    private LocalDate createdAt;

  public PostLike(Posting post, Long userId) {
    this.posting = post;
    this.userId = userId;
  }
}
