package com.may21.trobl.post.domain;

import com.may21.trobl._global.enums.PostingType;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Posting, Long> {

    @Query(
            """
                    SELECT p FROM Posting p LEFT JOIN p.postLikes l
                    LEFT JOIN p.poll poll
                    LEFT JOIN  poll.pollOptions
                    WHERE p.reported !=true AND p.postType = :postingType AND l.createdAt >= :startDate OR l IS NULL AND p.confirmed = true AND p.id NOT IN :blockedPostIds
                    ORDER BY SIZE(p.postLikes) DESC, p.viewCount DESC
                    LIMIT :count
                    """)
    List<Posting> findTopPostsByLikesAndViews(int count, LocalDate startDate, PostingType postingType, List<Long> blockedPostIds);

    @Query(
            """
                    SELECT p FROM Posting p LEFT JOIN p.postLikes l
                    LEFT JOIN p.poll poll
                    LEFT JOIN poll.pollOptions
                    WHERE p.reported !=true AND p.id NOT IN :blockedPostIds AND p.postType = :postingType AND p.confirmed = true ORDER BY SIZE(p.postLikes) DESC
                    LIMIT :count
                    """)
    List<Posting> findTopPostsByLikes(int count, PostingType postingType, List<Long> blockedPostIds);

    @Query("SELECT p FROM Posting p " + "WHERE p.reported !=true AND p.postType = :postingType AND p.id NOT IN :blockedPostIds AND p.confirmed = true ORDER BY p.viewCount DESC " + "LIMIT :count")
    List<Posting> findTopPostsByViews(int count, PostingType postingType, List<Long> blockedPostIds);

    @Query("""
            SELECT p FROM Posting p
            LEFT JOIN  p.poll poll
            LEFT JOIN  poll.pollOptions
            WHERE p.reported !=true AND p.postType = :postingType AND p.id NOT IN :blockedPostIds AND p.confirmed = true ORDER BY p.shareCount DESC LIMIT :count
            """)
    List<Posting> findTopPostsByShares(int count, PostingType postingType, List<Long> blockedPostIds);

    @Query(
            "SELECT p FROM Posting p LEFT JOIN p.comments l "
                    + "WHERE p.reported !=true AND p.postType = :postingType AND p.id NOT IN :blockedPostIds AND p.confirmed = true ORDER BY SIZE(p.comments) DESC "
                    + "LIMIT :count")
    List<Posting> findTopPostsByComments(int count, PostingType postingType, List<Long> blockedPostIds);

    @Query(""" 
            SELECT p
            FROM Posting p
            LEFT JOIN p.poll poll
                JOIN poll.pollOptions po
            JOIN po.pollVotes pv
                WHERE p.reported !=true AND p.id NOT IN :blockedPostIds AND p.confirmed = true
            GROUP BY p
            ORDER BY COUNT(pv) DESC
            LIMIT :count
            """)
    List<Posting> findTopPostsByVotes(int count, List<Long> blockedPostIds);


    Page<Posting> findByUserId(Long userId, Pageable pageRequest);

    @Query("""
                SELECT p
                FROM Posting p
                LEFT JOIN p.fairViews pv
                WHERE p.id = :postId
                GROUP BY p
                HAVING COUNT(pv) = 1
            """)
    Optional<Posting> getPostWithOnePairView(@Param("postId") Long postId);

    @Query(value = """
            SELECT * FROM posting p
            WHERE p.reported !=true AND post_type = :postingType AND p.confirmed = true 
                        AND (:#{#blockedPostIds.size()} = 0 OR p.id NOT IN :blockedPostIds)
            ORDER BY RANDOM() 
            LIMIT :count
            """, nativeQuery = true)
    List<Posting> findRandomPostsByType(@Param("count") int count, @Param("postingType") PostingType postingType, List<Long> blockedPostIds);

    @Query("SELECT p.id FROM Posting p LEFT JOIN p.postLikes l WHERE l.userId = :userId AND p.confirmed = true AND l.posting IN :list")
    List<Long> getAllIdsInListLikedByUserId(Long userId, List<Posting> list);


    @Query("SELECT p.id FROM Posting p LEFT JOIN p.views l WHERE l.userId = :userId AND p.confirmed = true AND l.posting IN :posts")
    List<Long> getAllIdsInListViewedByUserId(Long userId, List<Posting> posts);

    @Query("SELECT p.id FROM Posting p LEFT JOIN p.comments l WHERE l.userId = :userId AND p.confirmed = true AND l.posting IN :posts")
    List<Long> getAllIdsInListCommentedByUserId(Long userId, List<Posting> posts);


    @Query("SELECT p FROM Posting p WHERE p.confirmed !=true AND p.userId IN :userIds")
    Page<Posting> findAllUnconfirmedPostsByUserIdIn(List<Long> userIds, Pageable pageable);

    @Query("SELECT p FROM Posting p " +
            "LEFT JOIN p.poll poll " +
            "LEFT JOIN p.fairViews fairView " +
            "LEFT JOIN p.tags tag " +
            "WHERE p.reported !=true AND p.id NOT IN :blockedPostIds AND p.confirmed = true AND (" +
            "LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(poll.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(fairView.content) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(tag.tag.name) LIKE LOWER(CONCAT('%', :keyword, '%'))" +
            ")")
    List<Posting> searchByKeyword(@Param("keyword") String keyword, List<Long> blockedPostIds);


    List<Posting> findAllByUserId(Long userId);

    @Query("SELECT p FROM Posting p WHERE p.reported !=true AND p.id NOT IN :blockedPostIds AND p.confirmed = true")
    Page<Posting> findAllExceptBlocked(Pageable pageable, List<Long> blockedPostIds);
}
