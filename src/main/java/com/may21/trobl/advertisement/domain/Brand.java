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
public class Brand extends Timestamped {
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

    @OneToMany(mappedBy = "brand", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Advertisement> advertisements;

    public Brand(AdvertisementDto.BrandRequest brandRequest) {
        this.brandName = brandRequest.brandName();
        this.linkUrl = brandRequest.linkUrl();
        this.postId = brandRequest.postId();
        this.priority = brandRequest.priority();
        LocalDateTime now = LocalDateTime.now();
        
        // 문자열 날짜를 LocalDateTime으로 변환
        this.startDate = LocalDateTime.parse(brandRequest.startDate());
        this.endDate = LocalDateTime.parse(brandRequest.endDate());
        
        this.active = startDate.isBefore(now) && endDate.isAfter(now);
        this.dailyBudget = brandRequest.dailyBudget();
        this.costPerView = brandRequest.costPerView();
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