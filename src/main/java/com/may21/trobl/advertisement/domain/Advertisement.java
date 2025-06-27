package com.may21.trobl.advertisement.domain;

import com.may21.trobl._global.enums.AdType;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
public class Advertisement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String brandName;

    private String linkUrl;

    private Integer weight;

    private Integer priority;

    private Boolean active;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private Long dailyBudget;

    private Long costPerView;

    @OneToMany(mappedBy = "advertisement", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AdRecord> records;

    public String getImageUrl(AdType adType) {
        if (adType == AdType.BANNER_AD) {
            return "https://example.com/banner/" + id + ".jpg";
        } else if (adType == AdType.BIG_BANNER_AD) {
            return "https://example.com/interstitial/" + id + ".jpg";
        } else if (adType == AdType.FAIR_VIEW_AD) {
            return "https://example.com/native/" + id + ".jpg";
        }
        return null;
    }
}