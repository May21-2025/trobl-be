package com.may21.trobl.advertisement.domain;

import com.may21.trobl._global.component.GlobalValues;
import com.may21.trobl._global.enums.AdType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@EntityListeners(AuditingEntityListener.class)
@AllArgsConstructor
@NoArgsConstructor
public class AdRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Banner banner;

    private Long userId;

    private boolean clicked = false;

    private LocalDateTime showedAt;

    public AdRecord(Banner ad, Long userId) {
        this.banner = ad;
        this.userId = userId;
    }
}
