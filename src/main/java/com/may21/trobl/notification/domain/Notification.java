package com.may21.trobl.notification.domain;

import com.may21.trobl._global.enums.ItemType;
import com.may21.trobl._global.enums.NotificationType;
import com.may21.trobl.notification.dto.NotificationDto;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
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

    private ItemType itemType;
    private Long itemId;

    @Column(columnDefinition = "text")
    private String data;

    private Boolean read;

    private LocalDateTime scheduledTime;

    @CreatedDate
    private LocalDateTime createdAt;

    public Notification(Long userId, NotificationDto.SendRequest request,
            LocalDateTime scheduledTime) {
        this.userId = userId;
        this.type = request.getNotificationType();
        this.title = request.getTitle();
        this.body = request.getBody();
        this.itemType = request.getItemType();
        this.itemId = request.getItemId();
        this.data = request.getData() != null ? request.getData()
                .toString() : null;
        this.read = false;
        this.scheduledTime = scheduledTime != null ? scheduledTime : LocalDateTime.now();
    }

    public void markAsRead() {
        this.read = true;
    }
}

