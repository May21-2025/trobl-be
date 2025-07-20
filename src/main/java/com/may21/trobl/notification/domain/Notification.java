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

    @Column(columnDefinition = "text")
    private String data;
    private Boolean read;

    private LocalDateTime scheduledTime;

    @CreatedDate
    private LocalDateTime createdAt;

    public void markAsRead() {
        this.read = true;
    }
}

