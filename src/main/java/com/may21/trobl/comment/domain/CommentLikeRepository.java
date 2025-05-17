package com.may21.trobl.comment.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {


  @Modifying
  @Query("DELETE FROM CommentLike p WHERE p = :like")
  void deleteByEntity(CommentLike like);

  Optional<CommentLike> findByCommentIdAndUserId(Long commentId, Long userId);

  @Query("SELECT cl FROM CommentLike cl WHERE cl.userId = :userId AND cl.comment IN :comments")
  List<CommentLike> findByUserIdAndInComments(Long userId, List<Comment> comments);

  CommentLike findByUserIdAndComment(Long userId, Comment comment);
}
