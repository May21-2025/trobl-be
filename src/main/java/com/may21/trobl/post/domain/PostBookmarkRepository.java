package com.may21.trobl.post.domain;

import jakarta.persistence.OneToMany;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostBookmarkRepository extends JpaRepository<PostBookmark, Long> {

    Optional<PostBookmark> findByPostingIdAndUserId(Long postId, Long userId);

    boolean existsByPostingIdAndUserId(Long postId, Long id);

    @Query("SELECT p.posting FROM PostBookmark p WHERE p.userId = :userId")
    Page<Posting> findPostsByUserId(Long userId, Pageable pageable);
}
