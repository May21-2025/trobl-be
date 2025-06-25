package com.may21.trobl.advertisement.dto;

import com.may21.trobl._global.enums.AdType;
import com.may21.trobl._global.utility.Utility;
import com.may21.trobl.advertisement.domain.Advertisement;
import lombok.Getter;

public class AdvertisementDto {

    @Getter
    public static class Response{
        private String imageUrl;
        private String linkUrl;
        private String adType;

        public Response(Advertisement advertisement, AdType adType) {
            this.imageUrl = advertisement.getImageUrl(adType);
            this.linkUrl = advertisement.getLinkUrl();
            this.adType = adType.name();
        }

    }
}
