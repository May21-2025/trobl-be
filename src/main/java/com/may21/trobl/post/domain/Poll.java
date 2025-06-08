package com.may21.trobl.post.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class Poll {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Setter
    private String title;

    @Setter
    @OneToMany(
            mappedBy = "poll",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<PollOption> pollOptions;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    private Posting posting;

    public Poll(String pollTitle, Posting post) {
        this.title = pollTitle;
        this.posting = post;
    }


}
