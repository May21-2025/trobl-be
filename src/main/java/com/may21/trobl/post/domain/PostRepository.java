package com.may21.trobl.post.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.may21.trobl._global.enums.PostingType;
import com.may21.trobl.comment.domain.Comment;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Posting, Long> {

  @Query(
      """
SELECT p FROM Posting p LEFT JOIN p.postLikes l
LEFT JOIN FETCH p.poll poll
LEFT JOIN FETCH poll.pollOptions
WHERE p.postType = : postingType AND l.createdAt >= :startDate OR l IS NULL
ORDER BY SIZE(p.postLikes) DESC, p.viewCount DESC
LIMIT :count
""")
  List<Posting> findTopPostsByLikesAndViews(int count, LocalDate startDate, PostingType postingType);

  @Query(
      """
SELECT p FROM Posting p LEFT JOIN p.postLikes l
LEFT JOIN FETCH p.poll poll
LEFT JOIN FETCH poll.pollOptions
WHERE p.postType = :postingType ORDER BY SIZE(p.postLikes) DESC
LIMIT :count
""")
  List<Posting> findTopPostsByLikes(int count, PostingType postingType);

  @Query("SELECT p FROM Posting p " + "WHERE p.postType = :postingType ORDER BY p.viewCount DESC " + "LIMIT :count")
  List<Posting> findTopPostsByViews(int count, PostingType postingType);

  @Query("""
SELECT p FROM Posting p
LEFT JOIN FETCH p.poll poll
LEFT JOIN FETCH poll.pollOptions
WHERE p.postType = :postingType ORDER BY p.shareCount DESC LIMIT :count
""")
  List<Posting> findTopPostsByShares(int count, PostingType postingType);

  @Query(
      "SELECT p FROM Posting p LEFT JOIN p.comments l "
          + "WHERE p.postType = :postingType ORDER BY SIZE(p.comments) DESC "
          + "LIMIT :count")
  List<Posting> findTopPostsByComments(int count, PostingType postingType);

  @Query("""
    SELECT p
    FROM Posting p
    LEFT JOIN p.poll.pollOptions po
    LEFT JOIN po.pollVotes pv
    GROUP BY p
    ORDER BY COUNT(pv) DESC
    LIMIT :count
    """)
  List<Posting> findTopPostsByVotes(int count);

  @Query("SELECT p FROM Posting p JOIN p.comments c WHERE c IN :comments")
  List<Posting> findByIdInComments(List<Comment> comments);

  Page<Posting> findByUserId(Long userId, PageRequest pageRequest);
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
    SELECT * FROM posting 
    WHERE post_type = :postingType 
    ORDER BY RANDOM() 
    LIMIT :count
    """, nativeQuery = true)
  List<Posting> findRandomPostsByType( @Param("count") int count,@Param("postingType") PostingType postingType);

    @Query("SELECT p.id FROM Posting p LEFT JOIN p.postLikes l WHERE l.userId = :userId AND l.posting IN :list")
    List<Long> getAllIdsInListLikedByUserId(Long userId, List<Posting> list);
    @Query("SELECT p.id FROM Posting p LEFT JOIN p.bookmarks l WHERE l.userId = :userId")
  List<Long> getAllIdsInListBookmarkedByUserId(Long userId);

    @Query("SELECT p.id FROM Posting p LEFT JOIN p.views l WHERE l.userId = :userId AND l.posting IN :posts")
  List<Long> getAllIdsInListViewedByUserId(Long userId, List<Posting> posts);

    @Query("SELECT p.id FROM Posting p LEFT JOIN p.comments l WHERE l.userId = :userId AND l.posting IN :posts")
  List<Long> getAllIdsInListCommentedByUserId(Long userId, List<Posting> posts);
}
