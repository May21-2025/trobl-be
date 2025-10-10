package com.may21.trobl._global.enums;

public enum BannerType {

    SPLASH_AD, LANDING_AD, TOP_BANNER_AD, POST_AD, POLL_AD, FAIR_AD, EXIT_AD,
    ;

    public static BannerType fromString(String type) {
        return BannerType.valueOf(type.toUpperCase());
    }
}
