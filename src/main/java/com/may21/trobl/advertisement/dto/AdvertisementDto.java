package com.may21.trobl.advertisement.dto;

import com.may21.trobl.advertisement.domain.Brand;
import com.may21.trobl.advertisement.domain.Advertisement;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class AdvertisementDto {

    @Getter
    public static class Response {
        private final Long advertisementId;
        private final String imageUrl;
        private final String linkUrl;
        private final Long postId;
        private final String type;

        public Response(Brand advertisement, Advertisement banner) {
            this.advertisementId = banner.getId();
            this.imageUrl = banner.getImageUrl();
            this.linkUrl = advertisement.getLinkUrl();
            this.postId = advertisement.getId();
            this.type = banner.getBannerType()
                    .name();
        }

        public Response() {
            this.advertisementId = null;
            this.imageUrl = null;
            this.linkUrl = null;
            this.postId = null;
            this.type = null;
        }
    }


    @Getter
    public static class BannerList {
        private final BrandInfo brandInfo;
        private final List<AdvertisementInfo> advertisementInfos;

        public BannerList(BrandInfo brandInfo, List<AdvertisementInfo> advertisementInfos) {
            this.brandInfo = brandInfo;
            this.advertisementInfos = advertisementInfos;
        }
    }

    @Getter
    public static class AdvertisementInfo {
        private final Long advertisementId;
        private final String imageUrl;
        private final String bannerType;

        public AdvertisementInfo(Advertisement advertisement) {
            this.advertisementId = advertisement.getId();
            this.imageUrl = advertisement.getImageUrl();
            this.bannerType = advertisement.getBannerType()
                    .name();
        }

    }

    public record BrandRequest(String brandName, String linkUrl, Long postId,
                               Integer priority, Long dailyBudget, Long costPerView,
                               String startDate, String endDate) {}

    public record BannerRequest(String type, Integer weight) {}

    @Getter
    @AllArgsConstructor
    public static class BrandInfo {
        private final Long brandId;
        private final String brandName;
        private final String linkUrl;
        private final Long postId;
        private final Integer priority;
        private final Boolean active;
        private final Long dailyBudget;
        private final Long costPerView;
        private final LocalDateTime startDate;
        private final LocalDateTime endDate;

        public BrandInfo(Brand brand) {
            this.brandId = brand.getId();
            this.brandName = brand.getBrandName();
            this.linkUrl = brand.getLinkUrl();
            this.postId = brand.getPostId();
            this.priority = brand.getPriority();
            this.active = brand.getActive();
            this.dailyBudget = brand.getDailyBudget();
            this.costPerView = brand.getCostPerView();
            this.startDate = brand.getStartDate();
            this.endDate = brand.getEndDate();
        }

    }

    // 전체 광고 통계
    @Getter
    public static class AllStats {
        private final Long totalViews;           // 총 노출 수
        private final Long totalClicks;          // 총 클릭 수
        private final Double clickThroughRate;   // 클릭률 (CTR)
        private final Long totalUsers;           // 노출된 총 사용자 수
        private final Long activeAdvertisements; // 활성 광고 수
        private final List<DailyStats> dailyStats; // 일별 통계
        private final List<AdTypeStats> adTypeStats; // 광고 타입별 통계
        private final List<BrandStats> topBrands; // 상위 브랜드 통계

        public AllStats(Long totalViews, Long totalClicks, Double clickThroughRate, Long totalUsers,
                Long activeAdvertisements, List<DailyStats> dailyStats,
                List<AdTypeStats> adTypeStats, List<BrandStats> topBrands) {
            this.totalViews = totalViews;
            this.totalClicks = totalClicks;
            this.clickThroughRate = clickThroughRate;
            this.totalUsers = totalUsers;
            this.activeAdvertisements = activeAdvertisements;
            this.dailyStats = dailyStats;
            this.adTypeStats = adTypeStats;
            this.topBrands = topBrands;
        }
    }

    // 개별 광고 통계
    @Getter
    public static class AdStats {
        private final Long brandId;
        private final String brandName;
        private final Long totalViews;
        private final Long totalClicks;
        private final Double clickThroughRate;
        private final Long uniqueUsers;
        private final List<BannerStats> bannerStats; // 배너별 통계
        private final List<DailyStats> dailyStats;   // 일별 통계
        private final LocalDateTime startDate;
        private final LocalDateTime endDate;
        private final Boolean active;

        public AdStats(Long brandId, String brandName, Long totalViews, Long totalClicks,
                Double clickThroughRate, Long uniqueUsers, List<BannerStats> bannerStats,
                List<DailyStats> dailyStats, LocalDateTime startDate, LocalDateTime endDate,
                Boolean active) {
            this.brandId = brandId;
            this.brandName = brandName;
            this.totalViews = totalViews;
            this.totalClicks = totalClicks;
            this.clickThroughRate = clickThroughRate;
            this.uniqueUsers = uniqueUsers;
            this.bannerStats = bannerStats;
            this.dailyStats = dailyStats;
            this.startDate = startDate;
            this.endDate = endDate;
            this.active = active;
        }
    }


    // 배너별 통계
    @Getter
    public static class BannerStats {
        private final Long advertisementId;
        private final String type;
        private final Long views;
        private final Long clicks;
        private final Double clickThroughRate;
        private final String imageUrl;

        public BannerStats(Long advertisementId, String type, Long views, Long clicks,
                Double clickThroughRate, String imageUrl) {
            this.advertisementId = advertisementId;
            this.type = type;
            this.views = views;
            this.clicks = clicks;
            this.clickThroughRate = clickThroughRate;
            this.imageUrl = imageUrl;
        }
    }

    // 일별 통계
    @Getter
    public static class DailyStats {
        private final LocalDate date;
        private final Long views;
        private final Long clicks;
        private final Double clickThroughRate;
        private final Long uniqueUsers;

        public DailyStats(LocalDate date, Long views, Long clicks, Double clickThroughRate,
                Long uniqueUsers) {
            this.date = date;
            this.views = views;
            this.clicks = clicks;
            this.clickThroughRate = clickThroughRate;
            this.uniqueUsers = uniqueUsers;
        }
    }

    // 광고 타입별 통계
    @Getter
    public static class AdTypeStats {
        private final String type;
        private final Long views;
        private final Long clicks;
        private final Double clickThroughRate;
        private final Long advertisementCount;

        public AdTypeStats(String type, Long views, Long clicks, Double clickThroughRate,
                Long advertisementCount) {
            this.type = type;
            this.views = views;
            this.clicks = clicks;
            this.clickThroughRate = clickThroughRate;
            this.advertisementCount = advertisementCount;
        }
    }

    // 시간대별 통계 (추가 옵션)
    @Getter
    public static class HourlyStats {
        private final Integer hour;
        private final Long views;
        private final Long clicks;
        private final Double clickThroughRate;

        public HourlyStats(Integer hour, Long views, Long clicks, Double clickThroughRate) {
            this.hour = hour;
            this.views = views;
            this.clicks = clicks;
            this.clickThroughRate = clickThroughRate;
        }
    }

    // 사용자별 통계 (추가 옵션)
    @Getter
    public static class UserStats {
        private final Long userId;
        private final Long totalViews;
        private final Long totalClicks;
        private final Double clickThroughRate;
        private final List<String> viewedBrands;
        private final LocalDateTime firstViewDate;
        private final LocalDateTime lastViewDate;

        public UserStats(Long userId, Long totalViews, Long totalClicks, Double clickThroughRate,
                List<String> viewedBrands, LocalDateTime firstViewDate,
                LocalDateTime lastViewDate) {
            this.userId = userId;
            this.totalViews = totalViews;
            this.totalClicks = totalClicks;
            this.clickThroughRate = clickThroughRate;
            this.viewedBrands = viewedBrands;
            this.firstViewDate = firstViewDate;
            this.lastViewDate = lastViewDate;
        }
    }

    // 프로젝트별 전체 통계
    @Getter
    public static class BrandStats {
        private final String brandName;
        private final Long totalViews;
        private final Long totalClicks;
        private final Double clickThroughRate;
        private final Long totalUsers;
        private final Long activeAdvertisements;
        private final List<BrandDailyStats> dailyStats; // 일별 통계
        private final List<BrandHourlyStats> hourlyStats; // 시간별 통계
        private final List<AdTypeStats> adTypeStats; // 광고 타입별 통계
        private final List<BrandStats> topBrands; // 상위 브랜드 통계
        private final LocalDateTime firstViewDate;
        private final LocalDateTime lastViewDate;

        public BrandStats(String brandName, Long totalViews, Long totalClicks,
                Double clickThroughRate, Long totalUsers, Long activeAdvertisements,
                List<BrandDailyStats> dailyStats, List<BrandHourlyStats> hourlyStats,
                List<AdTypeStats> adTypeStats, List<BrandStats> topBrands,
                LocalDateTime firstViewDate, LocalDateTime lastViewDate) {
            this.brandName = brandName;
            this.totalViews = totalViews;
            this.totalClicks = totalClicks;
            this.clickThroughRate = clickThroughRate;
            this.totalUsers = totalUsers;
            this.activeAdvertisements = activeAdvertisements;
            this.dailyStats = dailyStats;
            this.hourlyStats = hourlyStats;
            this.adTypeStats = adTypeStats;
            this.topBrands = topBrands;
            this.firstViewDate = firstViewDate;
            this.lastViewDate = lastViewDate;
        }
    }

    // 프로젝트별 일별 통계
    @Getter
    public static class BrandDailyStats {
        private final String brandName;
        private final LocalDate date;
        private final Long views;
        private final Long clicks;
        private final Double clickThroughRate;
        private final Long uniqueUsers;
        private final List<AdTypeStats> adTypeBreakdown; // 광고 타입별 분석

        public BrandDailyStats(String brandName, LocalDate date, Long views, Long clicks,
                Double clickThroughRate, Long uniqueUsers, List<AdTypeStats> adTypeBreakdown) {
            this.brandName = brandName;
            this.date = date;
            this.views = views;
            this.clicks = clicks;
            this.clickThroughRate = clickThroughRate;
            this.uniqueUsers = uniqueUsers;
            this.adTypeBreakdown = adTypeBreakdown;
        }
    }

    // 프로젝트별 시간별 통계
    @Getter
    public static class BrandHourlyStats {
        private final String brandName;
        private final LocalDate date;
        private final Integer hour;
        private final Long views;
        private final Long clicks;
        private final Double clickThroughRate;
        private final Long uniqueUsers;

        public BrandHourlyStats(String brandName, LocalDate date, Integer hour, Long views,
                Long clicks, Double clickThroughRate, Long uniqueUsers) {
            this.brandName = brandName;
            this.date = date;
            this.hour = hour;
            this.views = views;
            this.clicks = clicks;
            this.clickThroughRate = clickThroughRate;
            this.uniqueUsers = uniqueUsers;
        }
    }

    // 프로젝트 목록 통계
    public record BrandListStats(List<BrandSummary> brands, Long totalBrands, Long totalViews,
                                 Long totalClicks, Double overallClickThroughRate) {}

    // 프로젝트 요약 정보
    @Getter
    public static class BrandSummary {
        private final String brandName;
        private final Long totalViews;
        private final Long totalClicks;
        private final Double clickThroughRate;
        private final Long activeAdvertisements;
        private final LocalDateTime lastActivity;

        public BrandSummary(String brandName, Long totalViews, Long totalClicks,
                Double clickThroughRate, Long activeAdvertisements, LocalDateTime lastActivity) {
            this.brandName = brandName;
            this.totalViews = totalViews;
            this.totalClicks = totalClicks;
            this.clickThroughRate = clickThroughRate;
            this.activeAdvertisements = activeAdvertisements;
            this.lastActivity = lastActivity;
        }
    }
}
