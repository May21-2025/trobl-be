package com.may21.trobl.notification.domain;

import com.may21.trobl._global.enums.ItemType;
import com.may21.trobl._global.enums.UpdateType;
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
public class ContentUpdate {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private Long userId;
    private Long targetId;
    @Enumerated(EnumType.STRING)
    private ItemType targetType;
    @Enumerated(EnumType.STRING)
    private UpdateType changeType;
    @CreatedDate
    private LocalDateTime createdAt;

    public ContentUpdate(Long userId, Long targetId, ItemType targetType, UpdateType changeType) {
        this.userId = userId;
        this.targetId = targetId;
        this.targetType = targetType;
        this.changeType = changeType;
    }
}
