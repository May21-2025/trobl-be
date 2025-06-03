package com.may21.trobl._global.enums;

public enum Language {
    KOR("ko", "한국어"),
    ENG("en", "English"),
    JPN("ja", "日本語");

    private final String code;
    private final String name;
    Language(String code, String name) {
        this.code = code;
        this.name = name;
    }
    public String getCode() {
        return code;
    }
    public String getName() {
        return name;
    }
}
