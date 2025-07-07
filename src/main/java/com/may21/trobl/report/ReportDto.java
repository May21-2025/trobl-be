package com.may21.trobl.report;

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

    }
}
