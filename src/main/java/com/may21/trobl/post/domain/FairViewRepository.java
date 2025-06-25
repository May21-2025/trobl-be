package com.may21.trobl.post.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FairViewRepository extends JpaRepository<FairView, Long> {

    @Query("SELECT f FROM FairView f WHERE f.posting.id = :postId AND f.userId = :userId")
    Optional<FairView> findByPostIdAndUserId(Long postId, Long userId);

    @Query("SELECT f FROM FairView f WHERE f.posting IN :postList")
    List<FairView> findAllByPostIn(List<Posting> postList);
}
