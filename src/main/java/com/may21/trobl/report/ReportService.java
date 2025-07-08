package com.may21.trobl.report;

import com.may21.trobl._global.enums.TargetType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ReportRepository reportRepository;

    public int report(Long userId, Long targetId, TargetType targetType, ReportDto.Request reportRequest) {
        if (!reportRepository.existsByTargetIdAndTargetTypeAndReportedBy(targetId, targetType, userId)) {

            Report report = new Report(targetId, targetType, userId, reportRequest);
            reportRepository.save(report);
        }
        return reportRepository.countReportByTargetIdAndTargetType(targetId, targetType) + 1;
    }

    public List<Long> getBlockedTargetIds(Long userId, List<Long> targetIds, TargetType targetType) {
        return reportRepository.findBlockedIdsByUserIdAndTargetTypeInTargetIds(userId, targetType, targetIds);
    }

    public List<Long> getBlockedTargetIds(Long userId, TargetType targetType) {
        return reportRepository.findIdsByReportedByAndTargetType(userId, targetType);
    }
}
