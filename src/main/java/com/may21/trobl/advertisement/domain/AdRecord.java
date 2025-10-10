package com.may21.trobl.advertisement.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
    private Advertisement advertisement;

    private Long userId;

    private String brandName;

    @Setter
    private boolean clicked = false;

    private LocalDateTime showedAt;

    public AdRecord(Advertisement ad, Long userId, String brandName) {
        this.advertisement = ad;
        this.userId = userId;
        this.showedAt = LocalDateTime.now();
        this.brandName = brandName;
    }

}
