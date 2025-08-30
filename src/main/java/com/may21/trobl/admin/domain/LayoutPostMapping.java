package com.may21.trobl.admin.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LayoutPostMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    private MainLayoutGroup mainLayoutGroup;

    private Long postId;

    public LayoutPostMapping(MainLayoutGroup mainLayoutGroup, Long postId) {
        this.mainLayoutGroup = mainLayoutGroup;
        this.postId = postId;
    }
}
