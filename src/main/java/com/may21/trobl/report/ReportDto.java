package com.may21.trobl.report;

import com.may21.trobl._global.enums.ReportType;
import lombok.Getter;

public class ReportDto {
    @Getter
    public class Request {
        private String reportReason;
        private String reportType;

        public Request(String reportReason, String reportType) {
            this.reportReason = reportReason;
            this.reportType = reportType;
        }

        public ReportType getReportType() {
            try {
                return ReportType.valueOf(reportType.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ReportType.ETC;
            }
        }

    }
}
