package com.may21.trobl.post.domain;

import jakarta.persistence.*;
import java.util.List;
import lombok.*;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class PollOption {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  private Long id;

  @Setter private String name;

  @Setter private Integer index;

  @OneToMany(
      mappedBy = "pollOption",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private List<PollVote> pollVotes;

  @ManyToOne(fetch = FetchType.LAZY)
  private Poll poll;

  @Builder
  public PollOption(String name, int index, Poll poll) {
    this.name = name;
    this.poll = poll;
    this.index = index;
  }

  public int getVoteCount() {
    return pollVotes == null ? 0 : pollVotes.size();
  }

  public int getIndex() {
    return index == null ? 0 : index;
  }
}
