package com.may21.trobl.advertisement.domain;

import com.may21.trobl.advertisement.dto.AdvertisementDto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
public class Advertisement {
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
        this.brandName = advertisementRequest.getBrandName();
        this.linkUrl = advertisementRequest.getLinkUrl();
        this.postId = advertisementRequest.getPostId();
        this.priority = advertisementRequest.getPriority();
        LocalDateTime now = LocalDateTime.now();
        this.startDate = advertisementRequest.getStartDate();
        this.endDate = advertisementRequest.getEndDate();
        this.active = startDate.isBefore(now) && endDate.isAfter(now);
        this.dailyBudget = advertisementRequest.getDailyBudget();
        this.costPerView = advertisementRequest.getCostPerView();
    }
}