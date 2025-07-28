package com.may21.trobl._global.enums;

public enum PostingType {
    GENERAL,
    POLL,
    FAIR_VIEW,
    ;

    public static PostingType fromString(String type) {
        return switch (type.toUpperCase()) {
            case "GENERAL" -> GENERAL;
            case "POLL" -> POLL;
            case "FAIR_VIEW" -> FAIR_VIEW;
            default -> throw new IllegalArgumentException("Unknown PostingType: " + type);
        };
    }
}
