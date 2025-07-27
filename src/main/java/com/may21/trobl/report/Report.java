package com.may21.trobl.report;

import com.may21.trobl._global.enums.ItemType;
import com.may21.trobl._global.enums.ReportType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "uk_report_unique_per_user_target", columnNames = {"reportedBy",
                "targetId", "targetType"})})
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long targetId;

    @Enumerated(EnumType.STRING)
    private ItemType targetType;

    private Long reportedBy;

    @Enumerated(EnumType.STRING)
    private ReportType reason;

    @CreatedDate
    private LocalDateTime reportedAt;

    public Report(Long targetId, ItemType targetType, Long userId,
            ReportDto.Request reportRequest) {
        this.targetId = targetId;
        this.targetType = targetType;
        this.reportedBy = userId;
        this.reason = reportRequest.getReportType();
    }
}
