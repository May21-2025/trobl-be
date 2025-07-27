package com.may21.trobl._global.enums;

public enum ItemType {

    USER,
    POST,
    COMMENT,
    ANNOUNCEMENT,
    REPORT;

    public static ItemType fromString(String itemType) {
        for (ItemType type : ItemType.values()) {
            if (type.name().equalsIgnoreCase(itemType)) {
                return type;
            }
        }
        return POST;
    }
}
