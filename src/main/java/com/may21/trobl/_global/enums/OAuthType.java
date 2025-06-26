package com.may21.trobl._global.enums;

public enum OAuthType {
    NONE("none"),
    GOOGLE("google"),
    APPLE("apple"),
    ;


    private final String title;


    OAuthType(String title) {
        this.title = title;
    }
}
