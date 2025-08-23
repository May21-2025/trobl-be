package com.may21.trobl.tag.domain;

import com.may21.trobl.post.domain.Posting;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "uk_tag_posting", columnNames = {"tag_id", "posting_id"})})
public class TagMapping {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Tag tag;

    @ManyToOne
    private Posting posting;

    private Boolean admin;

    public TagMapping(@NonNull Tag tag, @NonNull Posting post) {
        this.tag = tag;
        this.posting = post;
        this.admin = false;
    }

    public TagMapping(@NonNull Posting post, @NonNull Tag tag, boolean admin) {
        this.tag = tag;
        this.posting = post;
        this.admin = admin;
    }

    public void setTag(Tag remainingTag) {
        this.tag = remainingTag;
    }

    public boolean getAdmin() {
        return this.admin != null && this.admin;
    }
}
