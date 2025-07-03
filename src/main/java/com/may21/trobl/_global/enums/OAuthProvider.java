package com.may21.trobl._global.enums;

public enum OAuthProvider {
    NONE("none"),
    GOOGLE("google"),
    APPLE("apple"),
    KAKAO("kakao"),
    ;


    private final String title;


    OAuthProvider(String title) {
        this.title = title;
    }

    public static OAuthProvider fromString(String provider) {
        for (OAuthProvider oAuthProvider : OAuthProvider.values()) {
            if (oAuthProvider.title.equalsIgnoreCase(provider)) {
                return oAuthProvider;
            }
        }
        return NONE;
    }
}
