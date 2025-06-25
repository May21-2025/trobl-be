package com.may21.trobl._global.aop;

public record Threshold(int limit, String label, String message, String color, String bgColor, boolean urgent) {
    public boolean isUrgent() {
        return urgent;
    }
}
