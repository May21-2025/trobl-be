package com.may21.trobl.post.domain;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

  @Query("SELECT p.posting FROM PostLike p WHERE p.userId = :userId")
  Page<Posting> findPostingByUserId(Long userId, Pageable pageRequest);

  boolean existsByPostingIdAndUserId(Long postId, Long id);
}
