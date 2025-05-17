package com.may21.trobl.comment.domain;

import com.may21.trobl._global.utility.Timestamped;
import com.may21.trobl.post.domain.Posting;
import com.may21.trobl.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class Comment extends Timestamped {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private Long userId;

    @Setter
    @Column(columnDefinition = "text")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    private Posting posting;

    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommentLike> commentLikes;


    @Builder
   public Comment(User user, Posting post, String content) {
        this.userId = user.getId();
        this.content = content;
        this.posting = post;
    }

    public int getLikeCount() {
        return commentLikes ==null? 0 :commentLikes.size();
    }
}
