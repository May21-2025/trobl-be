package com.may21.trobl.comment.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment,Long> {

    @Query("SELECT c FROM Comment c WHERE c.posting.id = :postId ORDER BY c.createdAt ASC")
    List<Comment> findByPostId(Long postId);

    List<Comment> findByUserId(Long userId);
}
