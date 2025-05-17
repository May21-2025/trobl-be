package com.may21.trobl.post.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
  Optional<PostLike> findByPostingIdAndUserId(Long postId, Long userId);

  @Modifying
  @Query("DELETE FROM PostLike p WHERE p = :postLike")
  void deleteByEntity(PostLike postLike);

  List<Posting> findPostingByUserId(Long id);
}
