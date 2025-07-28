package com.may21.trobl.admin.announcement;

import com.may21.trobl.post.domain.Posting;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@EntityListeners(AuditingEntityListener.class)
@Table(uniqueConstraints = @UniqueConstraint(name = "unique_announcement_like", columnNames = {"userId",
        "announcement_id"}))

public class AnnouncementLike {
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @ManyToOne
    private Announcement announcement;

    public AnnouncementLike(Announcement announcement, Long userId) {
        this.announcement = announcement;
        this.userId = userId;
    }
}
