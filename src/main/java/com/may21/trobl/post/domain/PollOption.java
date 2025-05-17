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
  @Setter private String content;

  @Setter private Integer index;

  @OneToMany(
      mappedBy = "pollOption",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.EAGER)
  private List<PollVote> pollVotes;

  @ManyToOne(fetch = FetchType.LAZY)
  private Posting posting;

  @Builder
  public PollOption(String name, String content, int index, Posting post) {
    this.content = content;
    this.name = name;
    this.posting = post;
    this.index = index;
  }

  public long getVoteCount() {
    return pollVotes == null ? 0 : pollVotes.size();
  }

  public int getIndex() {
    return index == null ? 0 : index;
  }
}
