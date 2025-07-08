package com.may21.trobl.report;

import com.may21.trobl._global.enums.TargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    int countReportByTargetIdAndTargetType(Long postId, TargetType targetType);

    @Query("""
            SELECT r.targetId FROM Report r
            WHERE r.reportedBy = :userId AND r.targetType = :targetType AND r.targetId IN :targetIds
            """)
    List<Long> findBlockedIdsByUserIdAndTargetTypeInTargetIds(Long userId, TargetType targetType, List<Long> targetIds);

    @Query("""
            SELECT r.targetId FROM Report r
            WHERE r.reportedBy = :userId AND r.targetType = :targetType
            """)
    List<Long> findIdsByReportedByAndTargetType(Long userId, TargetType targetType);

    boolean existsByTargetIdAndTargetTypeAndReportedBy(Long targetId, TargetType targetType, Long reportedBy);
}
