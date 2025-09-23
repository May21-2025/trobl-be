package com.may21.trobl.post.domain;

import com.may21.trobl._global.enums.PostingType;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Posting, Long> {

    @Query("""
            SELECT p FROM Posting p LEFT JOIN p.postLikes l
            LEFT JOIN p.poll poll
            LEFT JOIN  poll.pollOptions
            WHERE p.reported !=true AND p.postType = :postType AND l.createdAt >= :startDate OR l IS NULL AND p.confirmed = true AND p.id NOT IN :blockedPostIds
            ORDER BY SIZE(p.postLikes) DESC, p.viewCount DESC
            LIMIT :count
            """)
    List<Posting> findTopPostsByLikesAndViews(int count, LocalDate startDate, PostingType postType,
            List<Long> blockedPostIds, List<Long> blockedUserIds);

    @Query("""
            SELECT p FROM Posting p LEFT JOIN p.postLikes l
            LEFT JOIN p.poll poll
            LEFT JOIN poll.pollOptions
            WHERE p.reported !=true AND p.id NOT IN :blockedPostIds AND p.userId NOT IN :blockedUserIds AND p.postType = :postType AND p.confirmed = true ORDER BY SIZE(p.postLikes) DESC
            LIMIT :count
            """)
    List<Posting> findTopPostsByLikes(int count, PostingType postType, List<Long> blockedPostIds,
            List<Long> blockedUserIds);

    @Query("SELECT p FROM Posting p " +
            "WHERE p.reported !=true AND p.postType = :postType AND p.id NOT IN :blockedPostIds  AND p.userId NOT IN :blockedUserIds AND p.confirmed = true ORDER BY p.viewCount DESC " +
            "LIMIT :count")
    List<Posting> findTopPostsByViews(int count, PostingType postType, List<Long> blockedPostIds,
            List<Long> blockedUserIds);

    @Query("""
            SELECT p FROM Posting p
            LEFT JOIN  p.poll poll
            LEFT JOIN  poll.pollOptions
            WHERE p.reported !=true AND p.postType = :postType AND p.id NOT IN :blockedPostIds  AND p.userId NOT IN :blockedUserIds AND p.confirmed = true ORDER BY p.shareCount DESC LIMIT :count
            """)
    List<Posting> findTopPostsByShares(int count, PostingType postType, List<Long> blockedPostIds,
            List<Long> blockedUserIds);

    @Query("SELECT p FROM Posting p LEFT JOIN p.comments l " +
            "WHERE p.reported !=true AND p.postType = :postType AND p.id NOT IN :blockedPostIds  AND p.userId NOT IN :blockedUserIds AND p.confirmed = true ORDER BY SIZE(p.comments) DESC " +
            "LIMIT :count")
    List<Posting> findTopPostsByComments(int count, PostingType postType, List<Long> blockedPostIds,
            List<Long> blockedUserIds);

    @Query(""" 
            SELECT p
            FROM Posting p
            LEFT JOIN p.poll poll
                JOIN poll.pollOptions po
            JOIN po.pollVotes pv
                WHERE p.reported !=true AND p.id NOT IN :blockedPostIds  AND p.userId NOT IN :blockedUserIds AND p.confirmed = true
            GROUP BY p
            ORDER BY COUNT(pv) DESC
            LIMIT :count
            """)
    List<Posting> findTopPostsByVotes(int count, List<Long> blockedPostIds,
            List<Long> blockedUserIds);


    Page<Posting> findByUserId(Long userId, Pageable pageRequest);


    @Query(value = """
            SELECT * FROM posting p
            WHERE p.reported !=true AND post_type = :postType AND p.confirmed = true 
                        AND (:#{#blockedPostIds.size()} = 0 OR p.id NOT IN :blockedPostIds)
                        AND (:#{#blockedUserIds.size()} = 0 OR p.id NOT IN :blockedUserIds)
            ORDER BY RANDOM() 
            LIMIT :count
            """, nativeQuery = true)
    List<Posting> findRandomPostsByType(@Param("count") int count,
            @Param("postType") PostingType postType, List<Long> blockedPostIds,
            List<Long> blockedUserIds);

    @Query("SELECT p FROM Posting p " +
            "WHERE p.reported != true AND p.postType = :postType AND p.confirmed = true " +
            "AND p.id NOT IN :blockedPostIds AND p.userId NOT IN :blockedUserIds " +
            "ORDER BY p.createdAt DESC " + "LIMIT :count")
    List<Posting> findRecentPollsByType(@Param("count") int count,
            @Param("postType") PostingType postType, List<Long> blockedPostIds,
            List<Long> blockedUserIds);


    @Query("SELECT p.id FROM Posting p LEFT JOIN p.postLikes l WHERE l.userId = :userId AND p.confirmed = true AND l.posting IN :list")
    List<Long> getAllIdsInListLikedByUserId(Long userId, List<Posting> list);


    @Query("SELECT p.id FROM Posting p LEFT JOIN p.views l WHERE l.userId = :userId AND p.confirmed = true AND l.posting IN :posts")
    List<Long> getAllIdsInListViewedByUserId(Long userId, List<Posting> posts);

    @Query("SELECT p.id FROM Posting p LEFT JOIN p.comments l WHERE l.userId = :userId AND p.confirmed = true AND l.posting IN :posts")
    List<Long> getAllIdsInListCommentedByUserId(Long userId, List<Posting> posts);


    @Query("SELECT p FROM Posting p WHERE p.confirmed !=true AND p.userId IN :userIds")
    Page<Posting> findAllUnconfirmedPostsByUserIdIn(List<Long> userIds, Pageable pageable);


    List<Posting> findAllByUserId(Long userId);

    @Query("SELECT p FROM Posting p WHERE p.postType !=:postType AND p.reported !=true AND p.id " +
            "NOT IN " + ":blockedPostIds" + " " + "AND p" +
            ".userId NOT IN :blockedUserIds AND p.confirmed = true")
    Page<Posting> findAllExceptBlocked(Pageable pageable, List<Long> blockedPostIds,
            List<Long> blockedUserIds, PostingType postType);

    List<Posting> findAllByReportedTrue();

    Optional<Posting> findByIdAndReportedIsTrue(Long postId);

    @Query("SELECT p.userId FROM Posting p WHERE p.id = :postId")
    Long findOwnerIdById(Long postId);

    @Query("SELECT p.userId FROM Posting p WHERE p.id = :postId")
    Long getPostOwnerIdByPostId(Long postId);

    @Query("SELECT p FROM Posting p WHERE p.id = :postId AND p.postType = :postType ")
    Optional<Posting> findByIdAndPostType(Long postId, PostingType postType);

    @Query("SELECT p.id FROM Posting p JOIN p.fairViews f WHERE f.id = :fairViewId")
    Long findPostIdByFairViewId(Long fairViewId);

    @Query("SELECT p FROM Posting p WHERE " +
            "p.reported != true AND p.id NOT IN :blockedPostIds AND " +
            "p.userId NOT IN :blockedUserIds AND p.confirmed = true AND p.postType = :postType")
    Page<Posting> findAllByPostType(PostingType postType, List<Long> blockedPostIds,
            List<Long> blockedUserIds, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Posting p WHERE p.postType !=:postType AND " +
            "p.reported != true AND p.id NOT IN :blockedPostIds AND " +
            "p.userId NOT IN :blockedUserIds AND p.confirmed = true AND " +
            "LOWER(p.title) = LOWER(:keyword)")
    List<Posting> searchByTitleExact(@Param("keyword") String keyword, List<Long> blockedPostIds,
            List<Long> blockedUserIds, PostingType postType);

    // 제목 부분 일치 검색 (높은 우선순위)
    @Query("SELECT DISTINCT p FROM Posting p WHERE p.postType !=:postType AND " +
            "p.reported != true AND p.id NOT IN :blockedPostIds AND " +
            "p.userId NOT IN :blockedUserIds AND p.confirmed = true AND " +
            "LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) AND " +
            "LOWER(p.title) != LOWER(:keyword)")
    List<Posting> searchByTitlePartial(@Param("keyword") String keyword, List<Long> blockedPostIds,
            List<Long> blockedUserIds, PostingType postType);

    // 내용 검색 (중간 우선순위)
    @Query("SELECT DISTINCT p FROM Posting p WHERE p.postType !=:postType AND " +
            "p.reported != true AND p.id NOT IN :blockedPostIds AND " +
            "p.userId NOT IN :blockedUserIds AND p.confirmed = true AND " +
            "LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Posting> searchByContent(@Param("keyword") String keyword, List<Long> blockedPostIds,
            List<Long> blockedUserIds, PostingType postType);

    // 태그 검색 (낮은 우선순위)
    @Query("SELECT DISTINCT p FROM Posting p JOIN p.tags t WHERE p.postType !=:postType AND " +
            "p.reported != true AND p.id NOT IN :blockedPostIds AND " +
            "p.userId NOT IN :blockedUserIds AND p.confirmed = true AND " +
            "LOWER(t.tag.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Posting> searchByTags(@Param("keyword") String keyword, List<Long> blockedPostIds,
            List<Long> blockedUserIds, PostingType postType);

    // Poll 제목 검색
    @Query("SELECT DISTINCT p FROM Posting p JOIN p.poll poll WHERE " +
            "p.reported != true AND p.id NOT IN :blockedPostIds AND " +
            "p.userId NOT IN :blockedUserIds AND p.confirmed = true AND " +
            "LOWER(poll.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Posting> searchByPollTitle(@Param("keyword") String keyword, List<Long> blockedPostIds,
            List<Long> blockedUserIds);

    // FairView 내용 검색
    @Query("SELECT DISTINCT p FROM Posting p JOIN p.fairViews fv WHERE " +
            "p.reported != true AND p.id NOT IN :blockedPostIds AND " +
            "p.userId NOT IN :blockedUserIds AND p.confirmed = true AND " +
            "LOWER(fv.content) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Posting> searchByFairViewContent(@Param("keyword") String keyword,
            List<Long> blockedPostIds, List<Long> blockedUserIds);

    @Query("SELECT COUNT(e) FROM Posting e WHERE e.postType != :postType AND e.createdAt > " +
            ":weekAgo")
    long countByCreatedAtAfter(LocalDateTime weekAgo, PostingType postType);

    @Query("SELECT p FROM Posting p WHERE p.userId IN :testUserIds")
    Page<Posting> findByUserIdIn(List<Long> testUserIds, Pageable pageable);

    @Query("SELECT p FROM Posting p WHERE p.postType IN :postingTypes")
    Page<Posting> findAllByPostTypeIn(List<PostingType> postingTypes, Pageable pageable);

    @Query("SELECT p FROM Posting p WHERE p.id != :id")
    List<Posting> findAllIdExceptAnnouncement(Long id);

    @Query("SELECT p FROM Posting p WHERE p.postType != :announcement AND (p.adminTagged IS NULL OR p.adminTagged = false) " +
            "AND p.confirmed = true")
    List<Posting> findAllHasNoAdminTagged(PostingType announcement);

    @Query("SELECT p FROM Posting p " +
            "WHERE p.postType != :postType AND p.createdAt > :localDateTime OR p.updatedAt > " +
            ":localDateTime")
    List<Posting> findAllByCreatedAtAfterOrUpdatedAtAfter(LocalDateTime localDateTime,
            PostingType postType);

    @Query("SELECT p.id FROM Posting p LEFT JOIN p.views v LEFT JOIN p.postLikes l LEFT JOIN p" +
            ".comments c WHERE p.postType != :postType AND p.createdAt < :localDateTime  AND (v" + ".createdAt " + "> " +
            ":localDateTime OR l.createdAt > :localDateTime OR c.createdAt > :localDateTime)")
    List<Long> findIdsByInteractedAtAfter(LocalDateTime localDateTime,
            PostingType postType);

    long countByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);

    long countByCreatedAtBetweenAndPostType(LocalDateTime startTime, LocalDateTime endTime, PostingType postType);
    
    // 일별 포스트 통계를 위한 배치 쿼리
    @Query("SELECT DATE(p.createdAt) as date, COUNT(p) as totalPosts " +
           "FROM Posting p " +
           "WHERE p.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(p.createdAt) " +
           "ORDER BY DATE(p.createdAt)")
    List<Object[]> getDailyPostStatsBetween(@Param("startDate") LocalDateTime startDate, 
                                           @Param("endDate") LocalDateTime endDate);
    
    // 일별 공정시청 포스트 통계
    @Query("SELECT DATE(p.createdAt) as date, COUNT(p) as fairViewPosts " +
           "FROM Posting p " +
           "WHERE p.createdAt BETWEEN :startDate AND :endDate " +
           "AND p.postType = :postType " +
           "GROUP BY DATE(p.createdAt) " +
           "ORDER BY DATE(p.createdAt)")
    List<Object[]> getDailyPostStatsBetween(@Param("startDate") LocalDateTime startDate,
                                                    @Param("endDate") LocalDateTime endDate, PostingType postType);
}
