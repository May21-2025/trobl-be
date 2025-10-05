package com.may21.trobl._global.enums;

public enum AdType {

    SPLASH_AD, LANDING_AD, TOP_BANNER_AD, POST_AD, POLL_AD, FAIR_AD, EXIT_AD,
    ;

    public static AdType fromString(String type) {
        return AdType.valueOf(type.toUpperCase());
    }
}
