package com.may21.trobl.advertisement.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
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

    private String brandName;

    private boolean clicked = false;

    private LocalDateTime showedAt;

    public AdRecord(Banner ad, Long userId, String brandName) {
        this.banner = ad;
        this.userId = userId;
        this.showedAt = LocalDateTime.now();
        this.brandName = brandName;
    }

}
