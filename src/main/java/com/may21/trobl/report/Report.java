package com.may21.trobl.report;

import com.may21.trobl._global.enums.ReportType;
import com.may21.trobl._global.enums.TargetType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long targetId;
    private TargetType targetType;

    private Long reportedBy;

    @Enumerated(EnumType.STRING)
    private ReportType reason;

    @Column(columnDefinition = "text")
    private String description;


    public Report(Long targetId, TargetType targetType, Long userId, ReportDto.Request reportRequest) {
        this.targetId = targetId;
        this.targetType = targetType;
        this.reportedBy = userId;
        this.reason = reportRequest.getReportType();
        this.description = reportRequest.getReportReason() != null ? reportRequest.getReportReason() : "";
    }
}
