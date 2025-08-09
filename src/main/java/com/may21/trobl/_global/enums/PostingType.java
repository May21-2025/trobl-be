package com.may21.trobl._global.enums;

public enum PostingType {
    GENERAL,
    POLL,
    FAIR_VIEW,
    ANNOUNCEMENT
    ;

    public static PostingType fromString(String type) {
        return switch (type.toUpperCase()) {
            case "GENERAL" -> GENERAL;
            case "POLL" -> POLL;
            case "FAIR_VIEW" -> FAIR_VIEW;
            case "ANNOUNCEMENT" -> ANNOUNCEMENT;
            default -> null;
        };
    }
}
