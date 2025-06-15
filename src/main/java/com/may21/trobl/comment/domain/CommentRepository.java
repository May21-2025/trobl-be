package com.may21.trobl.comment.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment,Long> {

    @Query("SELECT c FROM Comment c JOIN FETCH c.posting p WHERE p.id = :postId ORDER BY c.createdAt ASC")
    List<Comment> findByPostId(Long postId);

    Page<Comment> findByUserId(Long userId, Pageable pageRequest);

    @Query("SELECT c FROM Comment c JOIN FETCH c.posting p WHERE p.id IN :postIds")
    List<Comment> findByPostIdIn(List<Long> postIds);

    boolean existsByPostingIdAndUserId(Long postingId, Long userId);
}
