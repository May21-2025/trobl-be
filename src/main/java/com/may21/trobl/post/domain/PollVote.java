package com.may21.trobl.post.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@EntityListeners(AuditingEntityListener.class)
@Table(
    uniqueConstraints =
        @UniqueConstraint(
            name = "unique_user_poll_option",
            columnNames = {"userId", "pollOption_id"}))
public class PollVote {

  @Id
  @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
  private Long id;

  private Long userId;

  @ManyToOne private PollOption pollOption;

  @CreatedDate private LocalDate createdAt;

  public PollVote(PollOption pollOption, Long userId) {
    this.pollOption = pollOption;
    this.userId = userId;
  }
}
