package com.may21.trobl.admin.announcement;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@EntityListeners(AuditingEntityListener.class)
public class Announcement {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Setter
    private String title;

    @Setter
    @Column(columnDefinition = "text")
    private String content;

    @CreatedDate
    private LocalDateTime createAt;

    private long viewCount;
    private boolean isPinned;

    public Announcement(String title, String content) {
        this.title = title;
        this.content = content;
        this.viewCount = 0;
        this.isPinned = false;
    }
}
