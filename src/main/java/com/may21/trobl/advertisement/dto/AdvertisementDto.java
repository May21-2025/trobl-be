package com.may21.trobl.advertisement.dto;

import com.may21.trobl._global.enums.AdType;
import com.may21.trobl.advertisement.domain.Advertisement;
import com.may21.trobl.advertisement.domain.Banner;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

public class AdvertisementDto {

    @Getter
    public static class Response {
        private final Long bannerId;
        private final String imageUrl;
        private final String linkUrl;
        private final Long postId;
        private final String adType;

        public Response(Advertisement advertisement, Banner banner) {
            this.bannerId = banner.getId();
            this.imageUrl = banner.getImageUrl();
            this.linkUrl = advertisement.getLinkUrl();
            this.postId = advertisement.getId();
            this.adType = banner.getAdType()
                    .name();
        }

    }

    @Getter
    @AllArgsConstructor
    public static class CreateAdvertisement {
        private final AdvertisementRequest advertisementRequest;
        private final List<BannerRequest> bannerRequestList;
    }

    @Getter
    @AllArgsConstructor
    public static class BannerList {
        private final AdvertisementInfo advertisementInfo;
        private final List<BannerInfo> bannerInfos;
    }

    @Getter
    public static class BannerInfo {
        private final Long bannerId;
        private final String imageUrl;
        private final String adType;

        public BannerInfo(Banner banner) {
            this.bannerId = banner.getId();
            this.imageUrl = banner.getImageUrl();
            this.adType = banner.getAdType()
                    .name();
        }

    }
    @Getter
    @AllArgsConstructor
    public static class AdvertisementRequest {
        private final String brandName;
        private final String linkUrl;
        private final Long postId;
        private final Integer priority;
        private final Long dailyBudget;
        private final Long costPerView;
        private final LocalDateTime startDate;
        private final LocalDateTime endDate;

    }
    @Getter
    @AllArgsConstructor
    public static class BannerRequest {
        private final AdType type;
        private final Integer weight;
        private final String size;
    }

    @Getter
    @AllArgsConstructor
    public static class AdvertisementInfo {
        private final String brandName;
        private final String linkUrl;
        private final Long postId;
        private final Integer priority;
        private final Boolean active;
        private final Long dailyBudget;
        private final Long costPerView;
        private final LocalDateTime startDate;
        private final LocalDateTime endDate;

        public AdvertisementInfo(Advertisement advertisement) {
            this.brandName = advertisement.getBrandName();
            this.linkUrl = advertisement.getLinkUrl();
            this.postId = advertisement.getPostId();
            this.priority = advertisement.getPriority();
            this.active = advertisement.getActive();
            this.dailyBudget = advertisement.getDailyBudget();
            this.costPerView = advertisement.getCostPerView();
            this.startDate = advertisement.getStartDate();
            this.endDate = advertisement.getEndDate();
        }

    }
}
