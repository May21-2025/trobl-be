package com.may21.trobl._global.aop;

import java.util.List;

import static com.may21.trobl._global.component.AnsiColorCode.*;

public class Thresholds {
    public static final List<Threshold> QUERY_THRESHOLDS = List.of(
            new Threshold(100, "1", "Optimize Immediately", RED, RED_BG, true),
            new Threshold(50, "2", "Refactor Urgently", RED, PINK_BG, false),
            new Threshold(30, "3", "Inefficient Query Calls", ORANGE, ORANGE_BG, false),
            new Threshold(15, "4", "Warning Query Count High", YELLOW, YELLOW_BG, false)
    );

    public static final List<Threshold> TIME_THRESHOLDS = List.of(
            new Threshold(2000, "1", "Optimize Immediately", RED, PINK_BG, true),
            new Threshold(1000, "2", "Too Long", ORANGE, RED_BG, false),
            new Threshold(500, "3", "Consider Optimization", YELLOW, ORANGE_BG, false),
            new Threshold(300, "4", "Slight Delay", LIGHT_CYAN, YELLOW_BG, false)
    );

    public static Threshold evaluateQuery(int count) {
        return QUERY_THRESHOLDS.stream().filter(t -> count > t.limit()).findFirst().orElse(null);
    }

    public static Threshold evaluateTime(long millis) {
        return TIME_THRESHOLDS.stream().filter(t -> millis > t.limit()).findFirst().orElse(null);
    }
}
