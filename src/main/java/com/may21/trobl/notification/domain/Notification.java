package com.may21.trobl.notification.domain;

import com.may21.trobl._global.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private Long userId;

    private NotificationType type;

    private String title;
    private String body;

    private String data;

    private Boolean isRead;

    private LocalDateTime scheduledTime;

    @CreatedDate
    private LocalDateTime createdAt;

    public void markAsRead() {
        this.isRead = true;
    }

    @Builder
    public Notification(Long userId, NotificationType type, String title, String body, String data, LocalDateTime scheduledTime) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.data = data;
        this.isRead = false;
        this.scheduledTime = scheduledTime;
        this.createdAt = LocalDateTime.now();
    }
}

