package com.may21.trobl._global.enums;

public enum ScheduleType {
    NONE,
    DAILY,      // 매일
    WEEKLY,     // 매주
    MONTHLY,    // 매달
    YEARLY;     // 매년

    public static ScheduleType fromString(String scheduleType) {
        return switch (scheduleType.toUpperCase()) {
            case "DAILY" -> DAILY;
            case "WEEKLY" -> WEEKLY;
            case "MONTHLY" -> MONTHLY;
            case "YEARLY" -> YEARLY;
            default -> NONE;
        };
    }
}

