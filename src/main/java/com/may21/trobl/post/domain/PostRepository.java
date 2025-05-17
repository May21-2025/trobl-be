package com.may21.trobl.post.domain;

import java.time.LocalDate;
import java.util.List;

import com.may21.trobl.comment.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Posting, Long> {

  @Query(
      "SELECT p FROM Posting p LEFT JOIN p.postLikes l "
          + "WHERE l.createdAt >= :startDate OR l IS NULL "
          + "ORDER BY SIZE(p.postLikes) DESC, p.viewCount DESC "
          + "LIMIT :count")
  List<Posting> findTopPostsByLikesAndViews(int count, LocalDate startDate);

  @Query(
      "SELECT p FROM Posting p LEFT JOIN p.postLikes l "
          + "ORDER BY SIZE(p.postLikes) DESC "
          + "LIMIT :count")
  List<Posting> findTopPostsByLikes(int count);

  @Query("SELECT p FROM Posting p " + "ORDER BY p.viewCount DESC " + "LIMIT :count")
  List<Posting> findTopPostsByViews(int count);

  @Query("SELECT p FROM Posting p " + "ORDER BY p.shareCount DESC " + "LIMIT :count")
  List<Posting> findTopPostsByShares(int count);

  @Query(
      "SELECT p FROM Posting p LEFT JOIN p.comments l "
          + "ORDER BY SIZE(p.comments) DESC "
          + "LIMIT :count")
  List<Posting> findTopPostsByComments(int count);

  @Query("""
    SELECT p
    FROM Posting p
    LEFT JOIN p.pollOptions po
    LEFT JOIN po.pollVotes pv
    GROUP BY p
    ORDER BY COUNT(pv) DESC
    LIMIT :count
    """)
  List<Posting> findTopPostsByVotes(int count);

  @Query("SELECT p FROM Posting p JOIN p.comments c WHERE c IN :comments")
  List<Posting> findByIdInComments(List<Comment> comments);

  List<Posting> findByUserId(Long userId);
}
