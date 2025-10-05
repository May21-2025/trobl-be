package com.may21.trobl.advertisement.domain;

import com.may21.trobl._global.utility.Timestamped;
import com.may21.trobl.advertisement.dto.AdvertisementDto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
public class Advertisement extends Timestamped {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String brandName;

    private String linkUrl;
    private Long postId;


    private Integer priority;

    private Boolean active;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private Long dailyBudget;

    private Long costPerView;

    @OneToMany(mappedBy = "advertisement", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Banner> banners;

    public Advertisement(AdvertisementDto.AdvertisementRequest advertisementRequest) {
        this.brandName = advertisementRequest.brandName();
        this.linkUrl = advertisementRequest.linkUrl();
        this.postId = advertisementRequest.postId();
        this.priority = advertisementRequest.priority();
        LocalDateTime now = LocalDateTime.now();
        
        // 문자열 날짜를 LocalDateTime으로 변환
        this.startDate = LocalDateTime.parse(advertisementRequest.startDate());
        this.endDate = LocalDateTime.parse(advertisementRequest.endDate());
        
        this.active = startDate.isBefore(now) && endDate.isAfter(now);
        this.dailyBudget = advertisementRequest.dailyBudget();
        this.costPerView = advertisementRequest.costPerView();
    }

    // Getter methods
    public Long getId() {
        return id;
    }

    public String getBrandName() {
        return brandName;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public Long getPostId() {
        return postId;
    }

    public Integer getPriority() {
        return priority;
    }

    public Boolean getActive() {
        return active;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public Long getDailyBudget() {
        return dailyBudget;
    }

    public Long getCostPerView() {
        return costPerView;
    }
}