package com.may21.trobl.advertisement.domain;

import com.may21.trobl._global.component.GlobalValues;
import com.may21.trobl._global.enums.AdType;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

import static com.may21.trobl._global.component.GlobalValues.AD_IMAGE_PATH;

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
        String path = AD_IMAGE_PATH + "/" + brandName + "/" + adType.name().toLowerCase() + ".png";
        return GlobalValues.getCdnUrl() + path;
    }
}