package com.may21.trobl.notification.domain;

import com.may21.trobl.user.domain.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class NotificationSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @OneToOne
    private User user;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public NotificationSetting(User user) {
        this.user = user;
        this.updatedAt = LocalDateTime.now();
    }
}
