package com.may21.trobl.admin.announcement;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@EntityListeners(AuditingEntityListener.class)
@Table(uniqueConstraints = @UniqueConstraint(name = "unique_announcement_comment", columnNames = {
        "userId", "announcement_id"}))

public class AnnouncementComment {
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private String content;

    @ManyToOne
    private Announcement announcement;

    public AnnouncementComment(Announcement announcement, Long userId, String content) {
        this.announcement = announcement;
        this.content = content;
        this.userId = userId;
    }
}
