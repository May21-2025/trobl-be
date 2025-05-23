package com.may21.trobl.post.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostBookmarkRepository extends JpaRepository<PostBookmark, Long> {

    Optional<PostBookmark> findByPostingIdAndUserId(Long postId, Long userId);

    boolean existsByPostingIdAndUserId(Long postId, Long id);
}
