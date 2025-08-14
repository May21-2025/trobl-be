package com.may21.trobl.admin.domain;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class AdminTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long postId;

    @Column(columnDefinition = "text")
    private String tags;

    public AdminTag(Long id, List<String> strings) {
        this.postId = id;
        this.tags =  String.join(",", strings);
    }
    public String getTagString() {
        return tags;
    }

    public List<String> getTags() {
        List<String> tags = new ArrayList<>();
        if (this.tags == null) {
            return tags;
        }
        String[] tagArray = this.tags.split(",");
        for (String tag : tagArray) {
            tags.add(tag.trim());
        }
        return tags;
    }

    public void updateTags(List<String> tags) {
        this.tags = String.join(",", tags);
    }

}
