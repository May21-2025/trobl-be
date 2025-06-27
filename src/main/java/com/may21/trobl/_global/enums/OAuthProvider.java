package com.may21.trobl._global.enums;

public enum OAuthProvider {
    NONE("none"),
    GOOGLE("google"),
    APPLE("apple"),
    ;


    private final String title;


    OAuthProvider(String title) {
        this.title = title;
    }
}
