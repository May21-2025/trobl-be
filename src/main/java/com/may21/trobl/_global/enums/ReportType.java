package com.may21.trobl._global.enums;

public enum ReportType {
    ETC;

    public static ReportType fromStr(String reportType) {
        try {
            return ReportType.valueOf(reportType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ETC;
        }
    }
}
