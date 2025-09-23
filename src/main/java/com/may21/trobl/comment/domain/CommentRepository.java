package com.may21.trobl.comment.domain;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("SELECT c FROM Comment c JOIN FETCH c.posting p WHERE c.reported !=true AND p.id = :postId ORDER BY c.createdAt ASC")
    List<Comment> findByPostId(Long postId);

    Page<Comment> findByUserId(Long userId, Pageable pageRequest);

    @Query("SELECT c FROM Comment c JOIN FETCH c.posting p WHERE c.reported !=true AND  p.id IN :postIds")
    List<Comment> findByPostIdIn(List<Long> postIds);

    boolean existsByPostingIdAndUserId(Long postingId, Long userId);

    @Query("SELECT c.userId FROM Comment c WHERE c.id = :commentId")
    Long getOwnerIdByCommentId(Long commentId);

    List<Comment> findByReportedIsTrue();

    @Query("SELECT c FROM Comment c WHERE c.userId IN :testUserIds")
    Page<Comment> findAllByUserIdsIn(List<Long> testUserIds, Pageable pageable);

    int countByPostingIdAndReportedIsFalse(Long postId);

    long countByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    // 일별 댓글 통계를 위한 배치 쿼리
    @Query("SELECT DATE(c.createdAt) as date, COUNT(c) as commentCount " +
           "FROM Comment c " +
           "WHERE c.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(c.createdAt) " +
           "ORDER BY DATE(c.createdAt)")
    List<Object[]> getDailyCommentStatsBetween(@Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);
}
