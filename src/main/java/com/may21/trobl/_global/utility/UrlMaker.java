package com.may21.trobl._global.utility;

import com.may21.trobl._global.component.GlobalValues;
import com.may21.trobl._global.enums.AdType;
import com.may21.trobl.advertisement.domain.Advertisement;

import static com.may21.trobl._global.component.GlobalValues.AD_IMAGE_PATH;

public class UrlMaker {

    public static String makeAdImageUrl(String brandName, AdType adType) {
        return GlobalValues.getCdnUrl() + GlobalValues.getPREFIX() + makeAdImageUrlKey(brandName, adType);
    }

    public static String makeAdImageUrlKey(String brandName, AdType adType) {
        return AD_IMAGE_PATH + "/" + brandName +
                "/" + adType.name().toLowerCase() +
                ".webp";
    }
}
