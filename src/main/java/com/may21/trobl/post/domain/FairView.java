package com.may21.trobl.post.domain;

import com.may21.trobl.post.dto.PostDto;
import com.may21.trobl.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class FairView {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Setter
    private String title;

    @Column(columnDefinition = "text")
    @Setter
    private String content;

    private Long userId;
    @Setter
    private String nickname;

    @ManyToOne(fetch = FetchType.LAZY)
    private Posting posting;

    @Setter
    private boolean confirmed;


    @Builder
    public FairView(String title, String content, String nickname, Posting post, Long userId) {
        this.title = title;
        this.content = content;
        this.posting = post;
        this.userId = userId;
        this.nickname = nickname;
        this.confirmed = false;
    }

    public FairView(User partner, Posting post) {
        this.title = null;
        this.content = null;
        this.posting = post;
        this.userId = partner.getId();
        this.nickname = partner.getNickname();
        this.confirmed = false;
    }

    public void update(PostDto.FairViewRequest request) {
        if (request.getTitle() != null) {
            this.title = request.getTitle();
        }
        if (request.getContent() != null) {
            this.content = request.getContent();
        }
    }
}
