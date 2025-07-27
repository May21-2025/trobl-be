package com.may21.trobl.report;

import com.may21.trobl._global.enums.ItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    int countReportByTargetIdAndTargetType(Long postId, ItemType targetType);

    @Query("""
            SELECT r.targetId FROM Report r
            WHERE r.reportedBy = :userId AND r.targetType = :targetType AND r.targetId IN :targetIds
            """)
    List<Long> findBlockedIdsByUserIdAndTargetTypeInTargetIds(Long userId, ItemType targetType,
            List<Long> targetIds);

    @Query("""
            SELECT r.targetId FROM Report r
            WHERE r.reportedBy = :userId AND r.targetType = :targetType
            """)
    List<Long> findIdsByReportedByAndTargetType(Long userId, ItemType targetType);

    boolean existsByTargetIdAndTargetTypeAndReportedBy(Long targetId, ItemType targetType,
            Long reportedBy);

    @Query("""
            SELECT r FROM Report r
            WHERE r.reportedBy = :userId AND ((r.targetType = :targetType AND r.targetId IN :commentIds) OR (r.targetType = :userType))
            """)
    List<Report> getRelatedReports(Long userId, List<Long> commentIds, ItemType targetType,
            ItemType userType);

    @Query("""
             SELECT r FROM Report r
             WHERE (r.targetType = :postType AND r.targetId IN :reportedPosts)
                OR (r.targetType = :commentType AND r.targetId IN :reportedComments)
            """)
    List<Report> findAllByItemIdList(ItemType postType, List<Long> reportedPosts,
            ItemType commentType, List<Long> reportedComments);
}
