package com.may21.trobl.notification.domain;

import com.may21.trobl._global.enums.NotificationType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
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

    private LocalDateTime createdAt;

    public void markAsRead() {
        this.isRead = true;
    }

    @Builder
    public Notification(Long userId, NotificationType type, String title, String body, String data) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.data = data;
        this.isRead = false;
        this.createdAt = LocalDateTime.now();
    }
}

