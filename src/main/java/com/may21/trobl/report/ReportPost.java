package com.may21.trobl.report;

import com.may21.trobl._global.enums.ReportType;
import com.may21.trobl.post.domain.Posting;
import com.may21.trobl.post.dto.PostDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ReportPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long postId;
    private Long userId;

    private Long reportedBy;

    @Enumerated(EnumType.STRING)
    private ReportType reason;

    @Column(columnDefinition = "text")
    private String description;


    public ReportPost(Posting post, Long userId, PostDto.ReportRequest reportRequest) {
        this.postId = post.getId();
        this.userId = post.getUserId();
        this.reportedBy = userId;
        this.reason = ReportType.fromStr(reportRequest.getReportType());
        this.description = reportRequest.getReportReason() != null ? reportRequest.getReportReason() : "";

    }

}
