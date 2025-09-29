package com.may21.trobl.tag.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@NoArgsConstructor
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String name;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    private TagPool tagPool;

    @OneToMany(mappedBy = "tag", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TagKeyword> keywords;

    public void setName(String name) {
        this.name = name;
    }

    public Tag(String name) {
        this.name = name;
    }

    public Tag(String name, TagPool tagPool) {
        this.name = name;
        this.tagPool = tagPool;
    }

}
