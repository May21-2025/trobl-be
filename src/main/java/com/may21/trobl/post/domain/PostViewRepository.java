package com.may21.trobl.post.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PostViewRepository extends JpaRepository<PostView, Long> {

    @Query("""
            SELECT p.posting
            FROM PostView p
            WHERE p.userId = :userId
            """)
    Page<Posting> findPostingByUserId(Long userId, Pageable of);

    @Query("""
            SELECT COUNT(p) > 0
            FROM PostView p
            WHERE p.posting.id = :postId AND p.userId = :userId
            """)
    boolean existsByPostIdAndUserId(Long postId, Long userId);


    @Query("""
            SELECT p
            FROM PostView p
            WHERE p.posting IN :posts
            """)
    List<PostView> findAllByPosts(List<Posting> posts);

    int countByPostingId(Long postId);

    List<PostView> findAllByPostingIn(java.util.List<com.may21.trobl.post.domain.Posting> postList);

    @Query("SELECT p FROM PostView p WHERE p.posting.id IN :postIdList ")
    List<PostView> findAllByPostingIdIn(List<Long> postIdList);
}
