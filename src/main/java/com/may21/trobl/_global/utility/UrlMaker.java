package com.may21.trobl._global.utility;

import com.may21.trobl._global.component.GlobalValues;
import com.may21.trobl._global.enums.BannerType;

import static com.may21.trobl._global.component.GlobalValues.AD_IMAGE_PATH;

public class UrlMaker {

    public static String makeAdImageUrl(String brandName, BannerType bannerType) {
        return GlobalValues.getCdnUrl() + GlobalValues.getPREFIX() + makeAdImageUrlKey(brandName,
                bannerType);
    }

    public static String makeAdImageUrlKey(String brandName, BannerType bannerType) {
        return AD_IMAGE_PATH +  brandName +
                "/" + bannerType.name().toLowerCase() +
                ".webp";
    }
}
