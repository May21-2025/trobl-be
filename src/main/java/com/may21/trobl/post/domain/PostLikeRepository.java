package com.may21.trobl.post.domain;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    Optional<PostLike> findByPostingIdAndUserId(Long postId, Long userId);

    @Modifying
    @Query("DELETE FROM PostLike p WHERE p = :postLike")
    void deleteByEntity(PostLike postLike);

    @Query("SELECT p.posting FROM PostLike p WHERE p.userId = :userId")
    Page<Posting> findPostingByUserId(Long userId, Pageable pageRequest);

    boolean existsByPostingIdAndUserId(Long postId, Long id);

    List<PostLike> findAllByPostingId(Long postId);

    List<PostLike> findAllByPostingIn(java.util.List<com.may21.trobl.post.domain.Posting> postList);

    @Query("SELECT p FROM PostLike p WHERE p.posting.id IN :postIdList ")
    List<PostLike> findAllByPostingIdIn(List<Long> postIdList);

    long countByCreatedAtBetween(LocalDate startTime, LocalDate endTime);
    
    // 일별 좋아요 통계를 위한 배치 쿼리
    @Query("SELECT DATE(pl.createdAt) as date, COUNT(pl) as likeCount " +
           "FROM PostLike pl " +
           "WHERE pl.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(pl.createdAt) " +
           "ORDER BY DATE(pl.createdAt)")
    List<Object[]> getDailyLikeStatsBetween(@Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);
}
